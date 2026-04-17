package com.example.orderservice.dto;

import lombok.*;

import java.io.Serializable;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryCheckResponse implements Serializable {
    private Long productId;
    private boolean inStock;
    private Integer availableQuantity;
    private Long warehouseId;
}
