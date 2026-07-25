package com.nut753908.dbdesigndrill.service;

import com.nut753908.dbdesigndrill.dto.GenerateProblemResponse;
import com.nut753908.dbdesigndrill.dto.ReviewDesignResponse;
import com.nut753908.dbdesigndrill.dto.ReviewImplementationResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * AWS Lambda(AI連携)を呼び出さず、固定の応答を返す開発用スタブ。
 * Bedrockのクォータ承認待ちなど、AI連携なしで画面遷移だけを確認したい場合に
 * {@code app.lambda.stub-mode=true}(環境変数 {@code LAMBDA_STUB_MODE=true})で有効化する。
 */
@Component
@ConditionalOnProperty(prefix = "app.lambda", name = "stub-mode", havingValue = "true")
public class StubLambdaInvoker implements LambdaInvoker {

    @Override
    @SuppressWarnings("unchecked")
    public <T> T invoke(Object requestPayload, Class<T> responseType) {
        if (responseType == GenerateProblemResponse.class) {
            return (T) new GenerateProblemResponse(
                    "(スタブ応答)これはLAMBDA_STUB_MODEによるダミーの要件文です。");
        }
        if (responseType == ReviewDesignResponse.class) {
            return (T) new ReviewDesignResponse(
                    "(スタブ応答)これはLAMBDA_STUB_MODEによるダミーの設計レビューコメントです。",
                    "(スタブ応答)これはLAMBDA_STUB_MODEによるダミーの模範解答DDLです。");
        }
        if (responseType == ReviewImplementationResponse.class) {
            return (T) new ReviewImplementationResponse(
                    "(スタブ応答)これはLAMBDA_STUB_MODEによるダミーの実装レビューコメントです。");
        }
        throw new IllegalArgumentException("StubLambdaInvokerが対応していないレスポンス型です: " + responseType);
    }
}
