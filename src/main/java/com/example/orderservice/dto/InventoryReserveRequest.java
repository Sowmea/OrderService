package com.example.orderservice.dto;

import lombok.*;

import java.io.Serializable;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryReserveRequest implements Serializable {
    private String orderReference;
    private Long productId;
    private Integer quantity;
    private Long warehouseId;
}
