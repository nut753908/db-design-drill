# DB設計ドリル

DB設計(DDL)とSpring Data JPA実装を、生成AIのレビューを受けながら練習するための学習ツールです。

## 構成

```
backend/  Spring Boot (Java) アプリケーション本体
lambda/   AI連携用 Lambda関数 (Python)
```

- **backend**: 画面表示、お題・提出物・レビュー結果のCRUD、履歴管理を担当
- **lambda**: 生成AI(Google Gemini API)を呼び出し、お題生成・設計レビュー・実装レビューを行う

Spring BootからLambdaへは、AWS SDK for Javaによる同期 `invoke` 呼び出しで連携します(API Gatewayは使用しません)。

## 使い方の流れ

1. `/problems/new` でジャンル・難易度を選び、お題(要件文)を生成
2. お題に対してDDL(CREATE TABLE文)を入力して提出 → AIが設計をレビュー+模範解答を提示
3. 設計レビュー結果の画面からJPA実装コード(Entity/Repository)を提出 → AIが実装をレビュー
4. `/history` で過去のお題・提出・レビュー結果を確認

## セットアップ

### 前提

- Java 17 / Maven
- PostgreSQL
- AWS CLIの認証情報設定済み(Lambda呼び出し用)
- Google AI StudioのAPIキーを発行済み(詳細は`lambda/README.md`)

### 1. データベースを準備

```bash
createdb dbdesigndrill
```

### 2. Lambda関数をデプロイ

`lambda/README.md` の手順に従ってデプロイしてください。

### 3. Spring Bootを起動

```bash
cd backend
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export AWS_REGION=ap-northeast-1
export LAMBDA_FUNCTION_NAME=db-design-drill-ai
mvn spring-boot:run
```

起動後、`http://localhost:8080/` にアクセスしてください。

## 現バージョンのスコープ外(今後の拡張候補)

- DDLの実際の実行検証(構文チェック)
- 複数ユーザー対応・認証
- レビューの数値スコアリング
- AIレビューの非同期化(現状は同期呼び出しのため、応答まで数秒〜数十秒待つ)

## 動作確認について

このリポジトリのコードはJava/Maven環境が無い状態で作成したため、実際のビルド・起動確認はできていません。
`pom.xml` のAWS SDKバージョン等は依存解決時にMaven Centralの最新版へ調整が必要な場合があります。
`mvn spring-boot:run` 実行時にコンパイルエラーが出た場合は共有してください。
