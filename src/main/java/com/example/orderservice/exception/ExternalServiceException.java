package com.example.orderservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class ExternalServiceException extends RuntimeException {
    public ExternalServiceException(String serviceName, String detail) {
        super(String.format("External service [%s] unavailable: %s", serviceName, detail));
    }
    public ExternalServiceException(String serviceName, Throwable cause) {
        super(String.format("External service [%s] call failed", serviceName), cause);
    }
}
