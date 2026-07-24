package com.nut753908.dbdesigndrill.exception;

/** 指定されたIDのリソースが存在しない場合の例外 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
