package com.example.orderservice.dto;

import com.example.orderservice.enums.OrderStatus;
import com.example.orderservice.model.ShippingAddress;
import jakarta.validation.Valid;
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

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest implements Serializable {

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotBlank(message = "Customer email is required")
    @Email(message = "Customer email must be valid")
    private String customerEmail;

    @NotBlank(message = "Customer name is required")
    private String customerName;

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderItemRequest> items;

    @Valid
    @NotNull(message = "Shipping address is required")
    private ShippingAddressRequest shippingAddress;

    @Size(max = 3, min = 3, message = "Currency code must be 3 characters (e.g. USD)")
    @Builder.Default
    private String currencyCode = "USD";

    @DecimalMin(value = "0.0", message = "Discount cannot be negative")
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    private String paymentMethod;
    private String notes;
}

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

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse implements Serializable {

    private Long id;
    private String orderReference;
    private Long customerId;
    private String customerEmail;
    private String customerName;
    private OrderStatus status;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal shippingCost;
    private BigDecimal totalAmount;
    private String currencyCode;
    private String paymentMethod;
    private String paymentTransactionId;
    private String notes;
    private ShippingAddressResponse shippingAddress;
    private List<OrderItemResponse> orderItems;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime cancelledAt;

    public static OrderResponse from(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderReference(order.getOrderReference())
                .customerId(order.getCustomerId())
                .customerEmail(order.getCustomerEmail())
                .customerName(order.getCustomerName())
                .status(order.getStatus())
                .subtotal(order.getSubtotal())
                .taxAmount(order.getTaxAmount())
                .discountAmount(order.getDiscountAmount())
                .shippingCost(order.getShippingCost())
                .totalAmount(order.getTotalAmount())
                .currencyCode(order.getCurrencyCode())
                .paymentMethod(order.getPaymentMethod())
                .paymentTransactionId(order.getPaymentTransactionId())
                .notes(order.getNotes())
                .shippingAddress(ShippingAddressResponse.from(order.getShippingAddress()))
                .orderItems(order.getOrderItems()
                        .stream().map(OrderItemResponse::from).collect(Collectors.toList()))
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .confirmedAt(order.getConfirmedAt())
                .shippedAt(order.getShippedAt())
                .deliveredAt(order.getDeliveredAt())
                .cancelledAt(order.getCancelledAt())
                .build();
    }
}

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagedOrderResponse implements Serializable {
    private List<OrderResponse> orders;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}