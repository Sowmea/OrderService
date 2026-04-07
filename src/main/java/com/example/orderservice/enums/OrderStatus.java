package com.example.orderservice.enums;

/**
 * Lifecycle states of an Order.
 *
 *  PENDING ──► CONFIRMED ──► PROCESSING ──► SHIPPED ──► DELIVERED
 *     │            │              │
 *     └────────────┴──────────────┴──► CANCELLED
 *                                  └──► REFUNDED  (only from DELIVERED)
 */
public enum OrderStatus {

    /** Initial state – payment not yet verified. */
    PENDING,

    /** Payment confirmed; awaiting warehouse pick-up. */
    CONFIRMED,

    /** Warehouse is picking / packing the order. */
    PROCESSING,

    /** Order dispatched to carrier. */
    SHIPPED,

    /** Customer confirmed receipt (or carrier delivered). */
    DELIVERED,

    /** Order cancelled before shipment. */
    CANCELLED,

    /** Full or partial refund initiated after delivery. */
    REFUNDED,

    /** Terminal failure state – e.g. payment fraud, stock gap. */
    FAILED
}
