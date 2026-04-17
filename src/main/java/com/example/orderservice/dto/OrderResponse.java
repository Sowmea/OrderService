package com.example.orderservice.dto;

import com.example.orderservice.enums.OrderStatus;
import com.example.orderservice.model.Order;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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
