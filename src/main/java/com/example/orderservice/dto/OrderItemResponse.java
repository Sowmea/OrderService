package com.example.orderservice.dto;

import com.example.orderservice.model.OrderItem;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse implements Serializable {
    private Long id;
    private Long productId;
    private String productSku;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal unitDiscount;
    private BigDecimal lineTotal;
    private Long warehouseId;

    public static OrderItemResponse from(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productSku(item.getProductSku())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .unitDiscount(item.getUnitDiscount())
                .lineTotal(item.getLineTotal())
                .warehouseId(item.getWarehouseId())
                .build();
    }
}
