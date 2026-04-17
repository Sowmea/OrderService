package com.example.orderservice.dto;

import com.example.orderservice.enums.OrderStatus;
import com.example.orderservice.model.Order;
import com.example.orderservice.model.ShippingAddress;
import jakarta.validation.constraints.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

// ═══════════════════════════════════════════════════════════════════════════════
// REQUEST  DTOs
// ═══════════════════════════════════════════════════════════════════════════════

class _Requests {
}  // marker – grouping only

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class ShippingAddressRequest implements Serializable {
    @NotBlank(message = "Street is required")
    private String street;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Postal code is required")
    private String postalCode;

    @NotBlank(message = "Country is required")
    private String country;

    private String phone;
}

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class OrderItemRequest implements Serializable {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 9999, message = "Quantity must not exceed 9999")
    private Integer quantity;

    /**
     * Optional: pre-applied per-unit discount (coupon etc.)
     */
    @DecimalMin(value = "0.0", message = "Unit discount cannot be negative")
    private BigDecimal unitDiscount;
}

// ─── public-facing DTOs ───────────────────────────────────────────────────────

// ═══════════════════════════════════════════════════════════════════════════════
// RESPONSE  DTOs
// ═══════════════════════════════════════════════════════════════════════════════

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class OrderItemResponse implements Serializable {
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

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class ShippingAddressResponse implements Serializable {
    private String street;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String phone;

    public static ShippingAddressResponse from(ShippingAddress addr) {
        if (addr == null) return null;
        return ShippingAddressResponse.builder()
                .street(addr.getStreet()).city(addr.getCity())
                .state(addr.getState()).postalCode(addr.getPostalCode())
                .country(addr.getCountry()).phone(addr.getPhone())
                .build();
    }
}

