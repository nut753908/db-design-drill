import os

import boto3

MODEL_ID = os.environ["BEDROCK_MODEL_ID"]
MODEL_ANSWER_MARKER = "---MODEL_ANSWER---"

client = boto3.client("bedrock-runtime")


def lambda_handler(event, context):
    action = event.get("action")
    if action == "generate_problem":
        return generate_problem(event)
    if action == "review_design":
        return review_design(event)
    if action == "review_implementation":
        return review_implementation(event)
    raise ValueError(f"unknown action: {action}")


def generate_problem(event):
    genre = event["genre"]
    difficulty = event["difficulty"]
    prompt = f"""あなたはDB設計・JPA実装の演習問題を作成する講師です。
以下の条件で、演習用の要件文を1つ作成してください。

- ジャンル: {genre}
- 難易度: {difficulty}

要件文には以下を含めてください。
- 業務の概要
- 登場人物・エンティティの候補
- 満たすべき主要な業務ルール(3〜5個程度)

出力は要件文の本文のみとし、前置きや後書きは不要です。
"""
    text = call_bedrock(prompt)
    return {"requirementText": text}


def review_design(event):
    requirement_text = event["requirementText"]
    ddl_text = event["ddlText"]
    prompt = f"""あなたは経験豊富なDB設計のレビュアーです。
以下の要件文と、それに対して提出されたDDL(テーブル定義)をレビューしてください。

# 要件文
{requirement_text}

# 提出されたDDL
{ddl_text}

以下の観点でレビューコメントを作成してください。
- 正規化は適切か
- テーブル・カラムの命名は適切か
- リレーション(外部キー)の設計は適切か
- インデックス設計に問題はないか
- 要件に対して不足している要素はないか

レビューコメントの後に区切り線 "{MODEL_ANSWER_MARKER}" を出力し、
続けてあなたが考える模範解答のDDLを出力してください。
"""
    text = call_bedrock(prompt)
    review_comment, model_answer = split_model_answer(text)
    return {"reviewComment": review_comment, "modelAnswer": model_answer}


def review_implementation(event):
    requirement_text = event["requirementText"]
    ddl_text = event["ddlText"]
    code_text = event["codeText"]
    prompt = f"""あなたは経験豊富なSpring Data JPAのレビュアーです。
以下の要件文・DDL・JPA実装コードをレビューしてください。

# 要件文
{requirement_text}

# DDL(テーブル定義)
{ddl_text}

# 提出されたJPA実装コード
{code_text}

以下の観点でレビューコメントを作成してください。
- Entity/Repositoryの命名・責務分割は適切か
- リレーションのアノテーション(@OneToMany, @ManyToOne等)やFetchタイプは適切か
- N+1問題が発生しうる箇所はないか
- カスケード設定・削除時の挙動に問題はないか
- DDLとエンティティ定義の整合性は取れているか

出力はレビューコメントの本文のみとし、前置きや後書きは不要です。
"""
    text = call_bedrock(prompt)
    return {"reviewComment": text}


def call_bedrock(prompt):
    response = client.converse(
        modelId=MODEL_ID,
        messages=[{"role": "user", "content": [{"text": prompt}]}],
    )
    return response["output"]["message"]["content"][0]["text"]


def split_model_answer(text):
    if MODEL_ANSWER_MARKER in text:
        review_comment, model_answer = text.split(MODEL_ANSWER_MARKER, 1)
        return review_comment.strip(), model_answer.strip()
    return text.strip(), ""
