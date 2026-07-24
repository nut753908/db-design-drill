# db-design-drill-ai (Lambda)

DB設計ドリルのAI連携部分(お題生成・設計レビュー・実装レビュー)を担うLambda関数です。
Spring Bootアプリケーションから直接 `invoke` される想定で、API Gatewayは経由しません。

生成AIの呼び出しは **Google Gemini API**(Google AI Studio)経由で行います。
クレジットカード登録なしで使える無料枠があり、AWS Bedrock/Anthropic APIで発生していた
課金・クォータ関連の問題を回避できます。

## リクエスト/レスポンス形式

`action` フィールドで処理を分岐します。

| action | リクエスト | レスポンス |
|---|---|---|
| generate_problem | `genre`, `difficulty` | `requirementText` |
| review_design | `requirementText`, `ddlText` | `reviewComment`, `modelAnswer` |
| review_implementation | `requirementText`, `ddlText`, `codeText` | `reviewComment` |

## 事前準備: Google AI StudioでAPIキーを発行

1. https://aistudio.google.com/apikey をブラウザで開く(Googleアカウントでログイン)
2. 「Create API key」をクリックしてAPIキーを発行する
3. 使いたいモデル名を控えておく(例: `gemini-2.0-flash`。利用可能なモデルはAI Studioの「Models」ページで確認できます)

## デプロイ手順(手動・最小構成)

前提: AWS CLIの認証情報が設定済みであること。

```bash
# 1. 依存パッケージを同梱したデプロイパッケージを作成
cd lambda
rm -rf package function.zip
pip install -r requirements.txt -t package
cp handler.py package/
cd package && zip -r ../function.zip . && cd ..

# 2. Lambda実行ロールを作成(初回のみ。信頼ポリシーは lambda.amazonaws.com)
#    ログ出力権限(AWSLambdaBasicExecutionRole相当)のみで十分です。
#    Bedrockのように bedrock:InvokeModel 権限は不要です。

# 3. 関数を作成
aws lambda create-function \
  --function-name db-design-drill-ai \
  --runtime python3.12 \
  --handler handler.lambda_handler \
  --role <IAM_ROLE_ARN> \
  --zip-file fileb://function.zip \
  --timeout 60 \
  --memory-size 512 \
  --environment "Variables={GEMINI_API_KEY=<発行したAPIキー>,GEMINI_MODEL=gemini-2.0-flash}"

# 更新時は create-function の代わりに:
aws lambda update-function-code \
  --function-name db-design-drill-ai \
  --zip-file fileb://function.zip
```

Spring Boot側から呼び出すIAMユーザー/ロールには、この関数に対する
`lambda:InvokeFunction` 権限が必要です。

## IAM権限まとめ

- **Lambda実行ロール**: ログ出力権限(AWSLambdaBasicExecutionRole相当)のみ
- **Spring Bootが使う認証情報**: 対象Lambda関数への `lambda:InvokeFunction` 権限

## 環境変数まとめ

| 変数名 | 必須 | 説明 |
|---|---|---|
| `GEMINI_API_KEY` | 必須 | Google AI Studioで発行したAPIキー |
| `GEMINI_MODEL` | 必須 | 使用するGeminiモデル名(例: `gemini-2.0-flash`) |
