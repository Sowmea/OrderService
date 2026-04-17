package com.example.orderservice.dto;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse implements Serializable {
    private Long id;
    private String sku;
    private String name;
    private BigDecimal price;
    private BigDecimal taxRate;   // e.g. 0.08 for 8%
    private boolean active;
}

