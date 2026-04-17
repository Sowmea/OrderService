package com.example.orderservice.dto;

import lombok.*;

import java.io.Serializable;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryCheckRequest implements Serializable {
    private Long productId;
    private Integer quantityRequested;
}
