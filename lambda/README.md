# db-design-drill-ai (Lambda)

DB設計ドリルのAI連携部分(お題生成・設計レビュー・実装レビュー)を担うLambda関数です。
Spring Bootアプリケーションから直接 `invoke` される想定で、API Gatewayは経由しません。

生成AIの呼び出しは **Google Gemini API**(Generative Language API)経由で行います。呼び出しには
`generateContent` エンドポイントをHTTPS経由で直接叩く方式を採用しており、Lambdaランタイム
(python3.12)標準の `urllib.request` のみで動作するため、追加のパッケージ同梱は不要です。

## リクエスト/レスポンス形式

`action` フィールドで処理を分岐します。

| action | リクエスト | レスポンス |
|---|---|---|
| generate_problem | `genre`, `difficulty` | `requirementText` |
| review_design | `requirementText`, `ddlText` | `reviewComment`, `modelAnswer` |
| review_implementation | `requirementText`, `ddlText`, `codeText` | `reviewComment` |

## 事前準備

1. [Google AI Studio](https://aistudio.google.com/apikey) でAPIキーを発行する。従量課金
   (Pay-as-you-go)で使う場合は、紐づけるGoogle Cloudプロジェクトで課金を有効化しておくこと。
2. 使用するモデルIDを確認する(例: `gemini-2.5-flash`、`gemini-2.5-pro` など)。

## デプロイ手順(手動・最小構成)

前提: AWS CLIの認証情報が設定済みであること。

```bash
# 1. デプロイパッケージを作成(依存パッケージはランタイム標準のurllibのみで足りるため同梱不要)
cd lambda
rm -f function.zip
zip function.zip handler.py

# 2. Lambda実行ロールを作成(初回のみ。信頼ポリシーは lambda.amazonaws.com)
#    ログ出力権限(AWSLambdaBasicExecutionRole相当)のみで足りる(Bedrockのような
#    追加のAWS権限は不要)。

# 3. 関数を作成
aws lambda create-function \
  --function-name db-design-drill-ai \
  --runtime python3.12 \
  --handler handler.lambda_handler \
  --role <IAM_ROLE_ARN> \
  --zip-file fileb://function.zip \
  --timeout 60 \
  --memory-size 512 \
  --environment "Variables={GEMINI_API_KEY=<API_KEY>,GEMINI_MODEL_ID=gemini-2.5-flash}"

# 更新時は create-function の代わりに:
aws lambda update-function-code \
  --function-name db-design-drill-ai \
  --zip-file fileb://function.zip
```

Gemini APIの呼び出しはインターネット経由のHTTPS通信となるため、Lambda関数をVPC内に配置する
場合はNATゲートウェイ等、インターネットへの経路を確保しておくこと。Spring Boot側から呼び出す
IAMユーザー/ロールには、この関数に対する `lambda:InvokeFunction` 権限が別途必要。

## IAM権限まとめ

- **Lambda実行ロール**: ログ出力権限(AWSLambdaBasicExecutionRole相当)のみ
- **Spring Bootが使う認証情報**: 対象Lambda関数への `lambda:InvokeFunction` 権限

## 環境変数まとめ

| 変数名 | 必須 | 説明 |
|---|---|---|
| `GEMINI_API_KEY` | 必須 | Google AI Studioで発行したAPIキー |
| `GEMINI_MODEL_ID` | 必須 | 使用するGeminiのモデルID(例: `gemini-2.5-flash`) |
