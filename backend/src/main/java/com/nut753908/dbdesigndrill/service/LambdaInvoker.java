package com.nut753908.dbdesigndrill.service;

/** Spring BootからAI連携用Lambda関数を呼び出すための共通処理 */
public interface LambdaInvoker {

    <T> T invoke(Object requestPayload, Class<T> responseType);
}
