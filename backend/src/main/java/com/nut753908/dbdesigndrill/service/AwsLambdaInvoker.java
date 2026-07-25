package com.nut753908.dbdesigndrill.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nut753908.dbdesigndrill.exception.LambdaInvocationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

/** Spring BootからAI連携用Lambda関数を同期呼び出しするための実装 */
@Component
@ConditionalOnProperty(prefix = "app.lambda", name = "stub-mode", havingValue = "false", matchIfMissing = true)
public class AwsLambdaInvoker implements LambdaInvoker {

    private final LambdaClient lambdaClient;
    private final ObjectMapper objectMapper;
    private final String functionName;

    public AwsLambdaInvoker(
            LambdaClient lambdaClient,
            ObjectMapper objectMapper,
            @Value("${app.lambda.function-name}") String functionName) {
        this.lambdaClient = lambdaClient;
        this.objectMapper = objectMapper;
        this.functionName = functionName;
    }

    @Override
    public <T> T invoke(Object requestPayload, Class<T> responseType) {
        String requestJson = writeJson(requestPayload);

        InvokeRequest invokeRequest = InvokeRequest.builder()
                .functionName(functionName)
                .payload(SdkBytes.fromUtf8String(requestJson))
                .build();

        InvokeResponse invokeResponse = lambdaClient.invoke(invokeRequest);
        String responseJson = invokeResponse.payload().asUtf8String();

        if (invokeResponse.functionError() != null) {
            throw new LambdaInvocationException(
                    "Lambda関数がエラーを返しました(" + invokeResponse.functionError() + "): " + responseJson);
        }

        return readJson(responseJson, responseType);
    }

    private String writeJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new LambdaInvocationException("Lambdaリクエストのシリアライズに失敗しました", e);
        }
    }

    private <T> T readJson(String json, Class<T> responseType) {
        try {
            return objectMapper.readValue(json, responseType);
        } catch (JsonProcessingException e) {
            throw new LambdaInvocationException("Lambdaレスポンスの解析に失敗しました: " + json, e);
        }
    }
}
