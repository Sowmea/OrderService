package com.example.orderservice.controller;

import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.dto.PagedOrderResponse;
import com.example.orderservice.dto.UpdateOrderStatusRequest;
import com.example.orderservice.enums.OrderStatus;
import com.example.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * REST controller for the Order microservice.
 *
 * Base path : /api/v1/orders
 *
 * Endpoints:
 *   POST   /                          – Create order
 *   GET    /{id}                      – Get by ID
 *   GET    /ref/{reference}           – Get by order reference
 *   GET    /customer/{customerId}     – List by customer (paginated)
 *   GET    /                          – List all by status (admin)
 *   PATCH  /{id}/status               – Update order status
 *   PATCH  /{id}/cancel               – Cancel order
 *   DELETE /{id}                      – Hard-delete (CANCELLED/FAILED only)
 *   PUT    /{id}/cache/refresh        – Admin: warm/refresh cache entry
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    // ─── CREATE ───────────────────────────────────────────────────────────────

    /**
     * POST /api/v1/orders
     * Creates a new order. Returns 201 Created with Location header.
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {

        log.info("POST /orders – customer={}", request.getCustomerId());
        OrderResponse response = orderService.createOrder(request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    // ─── READ ─────────────────────────────────────────────────────────────────

    /**
     * GET /api/v1/orders/{id}
     * Served from Redis cache (cache-aside). Falls back to DB on miss.
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        log.debug("GET /orders/{}", id);
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    /**
     * GET /api/v1/orders/ref/{reference}
     * Lookup by human-readable order reference (e.g. ORD-AB12CD34).
     */
    @GetMapping("/ref/{reference}")
    public ResponseEntity<OrderResponse> getOrderByReference(
            @PathVariable String reference) {
        log.debug("GET /orders/ref/{}", reference);
        return ResponseEntity.ok(orderService.getOrderByReference(reference));
    }

    /**
     * GET /api/v1/orders/customer/{customerId}?page=0&size=20&sort=createdAt,desc
     * Paginated order history for a customer. Optional status filter.
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<PagedOrderResponse> getOrdersByCustomer(
            @PathVariable Long customerId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {

        Pageable pageable = buildPageable(page, size, sort);

        PagedOrderResponse response = (status != null)
                ? orderService.getOrdersByCustomerAndStatus(customerId, status, pageable)
                : orderService.getOrdersByCustomer(customerId, pageable);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/orders?status=PENDING&page=0&size=20
     * Admin: list all orders, optionally filtered by status.
     */
    @GetMapping
    public ResponseEntity<PagedOrderResponse> getAllOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {

        Pageable pageable = buildPageable(page, size, sort);

        PagedOrderResponse response = (status != null)
                ? orderService.getAllOrdersByStatus(status, pageable)
                : orderService.getAllOrdersByStatus(null, pageable);  // returns all

        return ResponseEntity.ok(response);
    }

    // ─── UPDATE STATUS ────────────────────────────────────────────────────────

    /**
     * PATCH /api/v1/orders/{id}/status
     * Transitions order to a new status (e.g. CONFIRMED, SHIPPED, DELIVERED).
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {

        log.info("PATCH /orders/{}/status → {}", id, request.getStatus());
        return ResponseEntity.ok(orderService.updateOrderStatus(id, request));
    }

    /**
     * PATCH /api/v1/orders/{id}/cancel
     * Cancels an order. Body is optional (cancellation reason).
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {

        log.info("PATCH /orders/{}/cancel – reason={}", id, reason);
        return ResponseEntity.ok(orderService.cancelOrder(id, reason));
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────

    /**
     * DELETE /api/v1/orders/{id}
     * Hard-deletes an order. Only allowed for CANCELLED or FAILED orders.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        log.info("DELETE /orders/{}", id);
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }

    // ─── CACHE MANAGEMENT ────────────────────────────────────────────────────

    /**
     * PUT /api/v1/orders/{id}/cache/refresh
     * Admin endpoint: forces a cache refresh for a specific order.
     */
    @PutMapping("/{id}/cache/refresh")
    public ResponseEntity<OrderResponse> refreshCache(@PathVariable Long id) {
        log.info("PUT /orders/{}/cache/refresh", id);
        return ResponseEntity.ok(orderService.refreshOrderCache(id));
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

    private Pageable buildPageable(int page, int size, String[] sort) {
        // Clamp page size to avoid abusive large requests
        int clampedSize = Math.min(size, 100);

        if (sort.length == 2) {
            Sort.Direction direction = Sort.Direction.fromOptionalString(sort[1])
                    .orElse(Sort.Direction.DESC);
            return PageRequest.of(page, clampedSize, Sort.by(direction, sort[0]));
        }
        return PageRequest.of(page, clampedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}