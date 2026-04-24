package com.example.orderservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(
	name = "order_items",
	indexes = {
		@Index(name = "idx_order_item_order_id", columnList = "order_id"),
		@Index(name = "idx_order_item_product_id", columnList = "product_id")
	}
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─── FK to Order ──────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_order_item_order"))
    private Order order;

    // ─── Product snapshot (denormalized at time of order) ─────────────────────

    @Column(name = "product_id", nullable = false)
    private Long productId;

    /**
     * Snapshot of the product SKU at order time — never changes post-order.
     */
    @Column(name = "product_sku", nullable = false, length = 100)
    private String productSku;

    /**
     * Snapshot of the product name at order time.
     */
    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    // ─── Pricing & Quantity ───────────────────────────────────────────────────

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /**
     * Unit price at time of purchase.
     */
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    /**
     * Discount per unit (coupon, promotion, etc.).
     */
    @Column(name = "unit_discount", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal unitDiscount = BigDecimal.ZERO;

    /**
     * (unitPrice - unitDiscount) * quantity
     */
    @Column(name = "line_total", nullable = false, precision = 19, scale = 4)
    private BigDecimal lineTotal;

    // ─── Fulfilment metadata ──────────────────────────────────────────────────

    /**
     * Warehouse / inventory location that will fulfil this line.
     */
    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "notes", length = 500)
    private String notes;

    // ─── Helpers ──────────────────────────────────────────────────────────────

    @PrePersist
    @PreUpdate
    public void calculateLineTotal() {
        BigDecimal discount = (this.unitDiscount != null) ? this.unitDiscount : BigDecimal.ZERO;
        this.lineTotal = (this.unitPrice.subtract(discount))
                .multiply(BigDecimal.valueOf(this.quantity));
    }
}