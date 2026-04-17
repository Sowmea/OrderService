package com.example.orderservice.dto;

import com.example.orderservice.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateOrderStatusRequest implements Serializable {

    @NotNull(message = "Status is required")
    private OrderStatus status;

    private String paymentTransactionId;
    private String notes;
}
