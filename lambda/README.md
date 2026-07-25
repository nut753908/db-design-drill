# db-design-drill-ai (Lambda)

DB設計ドリルのAI連携部分(お題生成・設計レビュー・実装レビュー)を担うLambda関数です。
Spring Bootアプリケーションから直接 `invoke` される想定で、API Gatewayは経由しません。

生成AIの呼び出しは **AWS Bedrock**(Anthropic Claude)経由で行います。呼び出しにはBedrock
Runtimeの `Converse API` を使用し、依存パッケージはLambdaランタイム(python3.12)に標準搭載の
`boto3` のみで動作するため、追加のパッケージ同梱は不要です。

## リクエスト/レスポンス形式

`action` フィールドで処理を分岐します。

| action | リクエスト | レスポンス |
|---|---|---|
| generate_problem | `genre`, `difficulty` | `requirementText` |
| review_design | `requirementText`, `ddlText` | `reviewComment`, `modelAnswer` |
| review_implementation | `requirementText`, `ddlText`, `codeText` | `reviewComment` |

## 事前準備

1. Bedrockのモデルアクセス設定で、使用するモデル(Anthropic Claude Sonnet 5)へのアクセスを
   有効化しておく(AWSコンソール > Amazon Bedrock > Model access)。
2. 使用するモデルIDを確認する。Claude Sonnet 5は `inferenceTypesSupported` が
   `INFERENCE_PROFILE` のみのため、素のモデルID(`anthropic.claude-sonnet-5`)ではオンデマンド
   呼び出しができない。必ずクロスリージョン推論プロファイルのIDを使うこと。
   ```bash
   aws bedrock list-inference-profiles --region <リージョン> \
     --query "inferenceProfileSummaries[?contains(inferenceProfileId, 'sonnet-5')]"
   ```
   本プロジェクトでは `global.anthropic.claude-sonnet-5` を使用している。
3. クロスリージョン推論はリクエスト量に応じてBedrockのService Quotas
   (`Cross-region model inference tokens per minute for Anthropic Claude Sonnet X`)の上限に
   達することがある。上限が低い場合はAWSコンソールのService Quotasからクォータ増加をリクエスト
   する。

## デプロイ手順(手動・最小構成)

前提: AWS CLIの認証情報が設定済みであること。

```bash
# 1. デプロイパッケージを作成(依存パッケージはランタイム標準のboto3のみで足りるため同梱不要)
cd lambda
rm -f function.zip
zip function.zip handler.py

# 2. Lambda実行ロールを作成(初回のみ。信頼ポリシーは lambda.amazonaws.com)
#    ログ出力権限(AWSLambdaBasicExecutionRole相当)に加え、
#    bedrock:InvokeModel 権限(対象モデル・推論プロファイルへの呼び出し)が必要。

# 3. 関数を作成
aws lambda create-function \
  --function-name db-design-drill-ai \
  --runtime python3.12 \
  --handler handler.lambda_handler \
  --role <IAM_ROLE_ARN> \
  --zip-file fileb://function.zip \
  --timeout 60 \
  --memory-size 512 \
  --environment "Variables={BEDROCK_MODEL_ID=global.anthropic.claude-sonnet-5}"

# 更新時は create-function の代わりに:
aws lambda update-function-code \
  --function-name db-design-drill-ai \
  --zip-file fileb://function.zip
```

Bedrockの呼び出しリージョンは、Lambda関数自体がデプロイされているリージョン(ランタイムが
自動設定する `AWS_REGION`)がそのまま使われる。Spring Boot側から呼び出すIAMユーザー/ロールには、
この関数に対する `lambda:InvokeFunction` 権限が別途必要。

## IAM権限まとめ

- **Lambda実行ロール**: ログ出力権限(AWSLambdaBasicExecutionRole相当)に加え、
  `bedrock:InvokeModel`(対象モデル・推論プロファイルのARN、もしくは `*`)
- **Spring Bootが使う認証情報**: 対象Lambda関数への `lambda:InvokeFunction` 権限

## 環境変数まとめ

| 変数名 | 必須 | 説明 |
|---|---|---|
| `BEDROCK_MODEL_ID` | 必須 | 使用するBedrockのモデルID/推論プロファイルID(例: `global.anthropic.claude-sonnet-5`) |
