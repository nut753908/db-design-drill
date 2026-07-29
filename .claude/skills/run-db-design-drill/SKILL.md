---
name: run-db-design-drill
description: Build, run, and drive db-design-drill (Spring Boot製のDB設計/JPA実装ドリルアプリ。AIレビューはAWS Lambda経由でGemini APIを呼ぶ)。db-design-drillの起動・画面のスクリーンショット取得・お題作成→DDL提出→設計レビュー→JPA実装提出→実装レビューという一連のフローの動作確認を頼まれたときに使う。
---

Spring Boot(`backend/`)のWebアプリ本体を起動し、`.claude/skills/run-db-design-drill/driver.mjs`
(Playwrightのヘッドレスブラウザドライバ)で実際に画面を操作して確認する。全パスはリポジトリ
ルート(`db-design-drill/`)からの相対パス。

## Prerequisites

```bash
java -version   # pom.xmlの指定は17だが、このコンテナのJDK 21でも問題なくビルド・起動できた
mvn -v
node -v         # ドライバ実行用。v22で動作確認済み
```

Playwrightのchromiumが未インストールの場合:

```bash
cd .claude/skills/run-db-design-drill
npx playwright install chromium
```

## Setup

### 1. PostgreSQLを用意する

このコンテナでは`postgresql.service`がsystemdで自動起動し、**既に5432番ポートを掴んでいる**
ことがある。`docker compose up -d db`を先に試すと `address already in use` で失敗するので、
まず既存のサービスを確認する。

```bash
pg_lsclusters
# 5432/onlineが動いていれば、docker composeは使わずそれをそのまま使う
PGPASSWORD=postgres psql -U postgres -h 127.0.0.1 -lqt | grep dbdesigndrill \
  || PGPASSWORD=postgres createdb -U postgres -h 127.0.0.1 dbdesigndrill
```

`5432`が空いている場合は、リポジトリの`docker-compose.yml`で代わりに起動する。

```bash
docker compose up -d db
docker exec db-design-drill-db-1 psql -U postgres -c "CREATE DATABASE dbdesigndrill" 2>/dev/null
```

### 2. AI連携(Lambda)の認証情報

生成AI呼び出しは`db-design-drill-ai`というLambda関数(中身はGemini API呼び出し)を経由する。
AWS CLIの認証情報が使えて、かつこの関数がデプロイ済み(`lambda/README.md`参照)であることが
前提。

```bash
aws lambda get-function --function-name db-design-drill-ai --query Configuration.State
# → "Active" ならOK
```

### 3. ドライバの依存パッケージ

```bash
cd .claude/skills/run-db-design-drill
npm install   # playwrightのみ。node_modulesは.gitignore済み
```

## Build

別途のビルド手順は不要。`mvn spring-boot:run`が初回にコンパイルまで行う。

## Run (agent path)

バックグラウンドでSpring Bootを起動し、`/history`が200を返すまで待つ:

```bash
cd backend
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export AWS_REGION=ap-northeast-1
export LAMBDA_FUNCTION_NAME=db-design-drill-ai
nohup mvn spring-boot:run > /tmp/db-design-drill-backend.log 2>&1 &
echo "PID: $!"

for i in {1..90}; do
  curl -sf http://localhost:8080/history > /dev/null 2>&1 && echo READY && break
  sleep 1
done
```

起動したらドライバを実行する。お題作成→DDL提出(設計レビュー)→JPA実装提出(実装レビュー)→
履歴確認までを1回のプロセスで最後まで通し、各画面のスクリーンショットを残す。

```bash
cd .claude/skills/run-db-design-drill
node driver.mjs
```

成功すると次の4行が出力される(IDは実行のたびに増える):

```
PROBLEM_URL: http://localhost:8080/problems/6
DESIGN_URL: http://localhost:8080/design-submissions/8
IMPL_URL: http://localhost:8080/implementation-submissions/4
DONE
```

スクリーンショットは `.claude/skills/run-db-design-drill/shots/` に出力される。

| ファイル | 画面 |
|---|---|
| `0_history_before.png` | 履歴一覧(実行前) |
| `1_new.png` | お題作成フォーム |
| `2_problem_show.png` | お題詳細(要件文・DDL入力欄) |
| `3_design_review.png` | 設計レビュー結果(AIレビュー・模範解答) |
| `4_implementation_review.png` | 実装レビュー結果 |
| `5_history_after.png` | 履歴一覧(実行後、お題が増えている) |

`BASE_URL`(デフォルト`http://localhost:8080`)と`SHOT_DIR`(デフォルトはこのディレクトリ直下の
`shots/`)は環境変数で上書きできる。

停止:

```bash
lsof -ti:8080 -sTCP:LISTEN | xargs -r kill
```

## Run (human path)

```bash
cd backend
export DB_USERNAME=postgres DB_PASSWORD=postgres
export AWS_REGION=ap-northeast-1 LAMBDA_FUNCTION_NAME=db-design-drill-ai
mvn spring-boot:run
```

`http://localhost:8080/`をブラウザで開く。AWS連携なしで画面遷移だけ確認したい場合は
`LAMBDA_STUB_MODE=true`を指定すると、Lambda呼び出しをせず固定のダミー応答が返る
(`README.md`参照)。停止は`Ctrl-C`。

## Test

`dbdesigndrill_test`データベースが必要(初回のみ作成)。PostgreSQLの用意はSetup手順1と同じ。

```bash
PGPASSWORD=postgres psql -U postgres -h 127.0.0.1 -lqt | grep dbdesigndrill_test \
  || PGPASSWORD=postgres createdb -U postgres -h 127.0.0.1 dbdesigndrill_test
cd backend
mvn test
```

→ `Tests run: 1, Failures: 0, Errors: 0`(`SubmissionFlowIntegrationTest`)。生成AI(Lambda)
呼び出しはこのテストではモック化されているため、AWS認証情報がなくても実行できる。

## Gotchas

- **`docker compose up -d db`が`address already in use`で失敗する**: このコンテナは
  `postgresql.service`がsystemdで既に5432番を掴んでいることがある。`pg_lsclusters`で確認し、
  動いていればそちらをそのまま使う(`docker compose`は使わない)。
- **設計/実装レビューの送信がまれにHTTP 502で失敗する**: Gemini API側の一時的な過負荷(503)が
  発生すると、`GlobalExceptionHandler`が`LambdaInvocationException`を502に変換して返す。
  フォームを送信し直せば大抵成功する。`driver.mjs`は`fillAndSubmitWithRetry`で最大3回まで
  自動的にフォーム再送信(同じ入力でページを開き直してから再送信。302は正常系なので5xxのみ
  リトライ対象)を行う。
- **`chromium-cli`はこのコンテナに未導入**: 代わりにPlaywrightを本スキル配下に直接
  `npm install`して`driver.mjs`を自作した。今後`chromium-cli`が使えるようになった場合、
  同じ操作手順で置き換え可能。
- **スクリーンショットの日本語がトーフ(四角)になる**: ヘッドレスChromiumに日本語フォントが
  入っていないための表示上の問題で、アプリのバグではない。文字化けの内容ではなく画面構造
  (フォームの有無、セクション構成)で正しさを判断すること。

## Troubleshooting

- **`Cannot find module 'playwright'`**: `.claude/skills/run-db-design-drill/`で`npm install`
  していない。そのディレクトリに`cd`してから実行する。
- **`curl: (7) Failed to connect`が`/history`ポーリングで続く**: `backend`の起動ログ
  (上記の`nohup`先、例: `/tmp/db-design-drill-backend.log`)を見る。大抵はDB接続エラー
  (`DB_USERNAME`/`DB_PASSWORD`不一致、`dbdesigndrill`データベース未作成)。
