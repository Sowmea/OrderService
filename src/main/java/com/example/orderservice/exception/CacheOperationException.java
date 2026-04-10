package com.example.orderservice.exception;

public class CacheOperationException extends RuntimeException {
    public CacheOperationException(String operation, Throwable cause) {
        super("Cache operation [" + operation + "] failed", cause);
    }
}