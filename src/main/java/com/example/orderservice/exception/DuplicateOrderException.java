package com.example.orderservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateOrderException extends RuntimeException {
    public DuplicateOrderException(String reference) {
        super("Order with reference already exists: " + reference);
    }
}
