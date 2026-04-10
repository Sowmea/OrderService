package com.example.orderservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InsufficientInventoryException extends RuntimeException {
    public InsufficientInventoryException(Long productId, int requested, int available) {
        super(String.format(
                "Insufficient inventory for product %d: requested=%d, available=%d",
                productId, requested, available));
    }
}
