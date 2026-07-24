package com.nut753908.dbdesigndrill.exception;

/** Lambda呼び出し・レスポンス変換に失敗した場合の例外 */
public class LambdaInvocationException extends RuntimeException {

    public LambdaInvocationException(String message) {
        super(message);
    }

    public LambdaInvocationException(String message, Throwable cause) {
        super(message, cause);
    }
}
