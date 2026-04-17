package com.example.orderservice.model;

import com.example.orderservice.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Core Order aggregate root. Owns OrderItems.
 * Serializable required for Redis cache serialization.
 */
@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_order_customer_id", columns = "customer_id"),
                @Index(name = "idx_order_status", columns = "status"),
                @Index(name = "idx_order_reference", columns = "order_reference", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "orderItems")
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    // ─── Primary Key ──────────────────────────────────────────────────────────

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    // ─── Business Key ─────────────────────────────────────────────────────────

    /**
     * Human-readable, external-safe reference (e.g. ORD-8f3a2b1c).
     */
    @Column(name = "order_reference", nullable = false, unique = true, length = 36)
    private String orderReference;

    // ─── Customer ─────────────────────────────────────────────────────────────

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "customer_email", nullable = false, length = 255)
    private String customerEmail;

    @Column(name = "customer_name", nullable = false, length = 255)
    private String customerName;

    // ─── Financials ───────────────────────────────────────────────────────────

    @Column(name = "subtotal", nullable = false, precision = 19, scale = 4)
    private BigDecimal subtotal;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal taxAmount;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "shipping_cost", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal shippingCost = BigDecimal.ZERO;

    /**
     * subtotal + taxAmount + shippingCost - discountAmount
     */
    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "currency_code", nullable = false, length = 3)
    @Builder.Default
    private String currencyCode = "USD";

    // ─── Shipping Address ─────────────────────────────────────────────────────

    @Embedded
    private ShippingAddress shippingAddress;

    // ─── Status & Lifecycle ───────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "payment_transaction_id", length = 100)
    private String paymentTransactionId;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ─── Timestamps ───────────────────────────────────────────────────────────

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    // ─── Version (Optimistic Locking) ─────────────────────────────────────────

    @Version
    @Column(name = "version")
    private Long version;

    // ─── Relationships ────────────────────────────────────────────────────────

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    // ─── Helpers ──────────────────────────────────────────────────────────────

    public void addOrderItem(OrderItem item) {
        item.setOrder(this);
        this.orderItems.add(item);
    }

    public void removeOrderItem(OrderItem item) {
        this.orderItems.remove(item);
        item.setOrder(null);
    }

    /**
     * Recalculates totalAmount from component fields.
     */
    public void recalculateTotal() {
        this.subtotal = orderItems.stream()
                .map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.totalAmount = this.subtotal
                .add(this.taxAmount)
                .add(this.shippingCost)
                .subtract(this.discountAmount);
    }

    @PrePersist
    private void prePersist() {
        if (this.orderReference == null || this.orderReference.isBlank()) {
            this.orderReference = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        if (this.taxAmount == null) this.taxAmount = BigDecimal.ZERO;
        if (this.discountAmount == null) this.discountAmount = BigDecimal.ZERO;
        if (this.shippingCost == null) this.shippingCost = BigDecimal.ZERO;
    }
}