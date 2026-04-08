package com.example.orderservice.service;

import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.dto.PagedOrderResponse;
import com.example.orderservice.dto.UpdateOrderStatusRequest;
import com.example.orderservice.enums.OrderStatus;
import com.example.orderservice.model.ShippingAddress;
import com.example.orderservice.repository.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * OrderService – Business orchestration for the Order microservice.
 * <p>
 * Cache strategy: CACHE-ASIDE (Read-Through pattern via Spring @Cacheable).
 * <p>
 * READ  → check Redis first; on miss load from DB and populate cache.
 * WRITE → persist to DB first, then evict/update cache to keep consistency.
 * <p>
 * External calls:
 * - ProductService  : fetch price & product metadata.
 * - InventoryService: check stock, reserve, and release.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductServiceClient productServiceClient;
    private final InventoryServiceClient inventoryServiceClient;
    private final RedisTemplate<String, Object> redisTemplate;

    // ═══════════════════════════════════════════════════════════════════════
    // CREATE
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Creates a new order.
     * Flow:
     * 1. Validate products via ProductService (prices, active flag).
     * 2. Validate inventory via InventoryService (batch check).
     * 3. Persist order + items.
     * 4. Reserve inventory.
     * 5. Populate cache.
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating order for customer={}, items={}",
                request.getCustomerId(), request.getItems().size());

        // ── 1. Fetch product details ────────────────────────────────────────
        List<Long> productIds = request.getItems().stream()
                .map(CreateOrderRequest.OrderItemRequest::getProductId)
                .collect(Collectors.toList());

        Map<Long, ProductResponse> productMap = fetchProductMap(productIds);
        validateProducts(request.getItems(), productMap);

        // ── 2. Validate inventory (batch) ───────────────────────────────────
        List<InventoryCheckResponse> stockChecks = checkInventoryBatch(request.getItems());
        Map<Long, InventoryCheckResponse> stockMap = stockChecks.stream()
                .collect(Collectors.toMap(InventoryCheckResponse::getProductId, r -> r));
        validateStock(request.getItems(), stockMap);

        // ── 3. Build Order aggregate ────────────────────────────────────────
        Order order = buildOrder(request, productMap, stockMap);
        Order saved = orderRepository.save(order);

        // ── 4. Reserve inventory (best-effort; compensate on failure) ────────
        try {
            reserveInventoryForOrder(saved, stockMap);
        } catch (ExternalServiceException ex) {
            log.error("Inventory reservation failed for order {}; marking FAILED",
                    saved.getOrderReference(), ex);
            saved.setStatus(OrderStatus.FAILED);
            orderRepository.save(saved);
            throw ex;
        }

        // ── 5. Populate cache ───────────────────────────────────────────────
        OrderResponse response = OrderResponse.from(saved);
        putInCache(saved.getId(), saved.getOrderReference(), response);

        log.info("Order created: id={}, ref={}", saved.getId(), saved.getOrderReference());
        return response;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // READ (Cache-Aside)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Cache-aside: check Redis, fall back to DB on miss.
     * Spring @Cacheable handles the aside logic automatically.
     */
    @Cacheable(
            cacheNames = RedisConfig.CACHE_ORDER_BY_ID,
            key = "#id",
            unless = "#result == null"
    )
    public OrderResponse getOrderById(Long id) {
        log.debug("Cache MISS – loading order id={} from DB", id);
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        return OrderResponse.from(order);
    }

    /**
     * Lookup by human-readable reference (ORD-XXXX).
     */
    @Cacheable(
            cacheNames = RedisConfig.CACHE_ORDER_BY_REFERENCE,
            key = "#reference",
            unless = "#result == null"
    )
    public OrderResponse getOrderByReference(String reference) {
        log.debug("Cache MISS – loading order ref={} from DB", reference);
        Order order = orderRepository.findByOrderReferenceWithItems(reference)
                .orElseThrow(() -> new OrderNotFoundException(reference));
        return OrderResponse.from(order);
    }

    /**
     * Paginated list for a customer.
     * Short TTL (5 min) avoids stale list after new order.
     */
    @Cacheable(
            cacheNames = RedisConfig.CACHE_ORDER_LIST_CUSTOMER,
            key = "#customerId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize"
    )
    public PagedOrderResponse getOrdersByCustomer(Long customerId, Pageable pageable) {
        log.debug("Fetching orders for customer={}", customerId);
        Page<Order> page = orderRepository.findByCustomerId(customerId, pageable);
        return toPagedResponse(page, pageable);
    }

    /**
     * Orders by customer + status – not cached (high cardinality key space).
     */
    public PagedOrderResponse getOrdersByCustomerAndStatus(
            Long customerId, OrderStatus status, Pageable pageable) {
        Page<Order> page = orderRepository.findByCustomerIdAndStatus(customerId, status, pageable);
        return toPagedResponse(page, pageable);
    }

    /**
     * Admin: all orders by status (paginated).
     */
    public PagedOrderResponse getAllOrdersByStatus(OrderStatus status, Pageable pageable) {
        Page<Order> page = orderRepository.findByStatus(status, pageable);
        return toPagedResponse(page, pageable);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // UPDATE STATUS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Transitions an order to a new status with validation.
     * Evicts cache on success.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = RedisConfig.CACHE_ORDER_BY_ID, key = "#id"),
            @CacheEvict(cacheNames = RedisConfig.CACHE_ORDER_LIST_CUSTOMER, allEntries = true)
    })
    public OrderResponse updateOrderStatus(Long id, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        validateStatusTransition(order.getStatus(), request.getStatus());

        OrderStatus previous = order.getStatus();
        order.setStatus(request.getStatus());

        // Apply status-specific timestamps and side-effects
        applyStatusSideEffects(order, request);

        // If cancelled – release inventory
        if (request.getStatus() == OrderStatus.CANCELLED
                || request.getStatus() == OrderStatus.FAILED) {
            releaseInventoryForOrder(order);
        }

        Order saved = orderRepository.save(order);

        // Evict reference cache key too
        evictReferenceCache(saved.getOrderReference());

        log.info("Order {} transitioned {} → {}", id, previous, request.getStatus());
        return OrderResponse.from(saved);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CANCEL
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = RedisConfig.CACHE_ORDER_BY_ID, key = "#id"),
            @CacheEvict(cacheNames = RedisConfig.CACHE_ORDER_LIST_CUSTOMER, allEntries = true)
    })
    public OrderResponse cancelOrder(Long id, String reason) {
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        Set<OrderStatus> cancellableStatuses =
                Set.of(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PROCESSING);

        if (!cancellableStatuses.contains(order.getStatus())) {
            throw new InvalidOrderStatusTransitionException(
                    order.getStatus().name(), OrderStatus.CANCELLED.name());
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        if (reason != null && !reason.isBlank()) {
            order.setNotes(reason);
        }

        releaseInventoryForOrder(order);

        Order saved = orderRepository.save(order);
        evictReferenceCache(saved.getOrderReference());

        log.info("Order {} cancelled. Reason: {}", id, reason);
        return OrderResponse.from(saved);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DELETE (soft-cancel only; hard delete for admin)
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = RedisConfig.CACHE_ORDER_BY_ID, key = "#id"),
            @CacheEvict(cacheNames = RedisConfig.CACHE_ORDER_LIST_CUSTOMER, allEntries = true)
    })
    public void deleteOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        // Safety: only delete CANCELLED or FAILED orders
        if (order.getStatus() != OrderStatus.CANCELLED
                && order.getStatus() != OrderStatus.FAILED) {
            throw new InvalidOrderStatusTransitionException(
                    order.getStatus().name(), "DELETED");
        }

        evictReferenceCache(order.getOrderReference());
        orderRepository.delete(order);
        log.info("Order {} hard-deleted", id);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CACHE MANAGEMENT (manual / admin)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Manually warm cache for a single order (e.g. after admin update).
     */
    public OrderResponse refreshOrderCache(Long id) {
        log.info("Refreshing cache for order id={}", id);
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        OrderResponse response = OrderResponse.from(order);
        putInCache(order.getId(), order.getOrderReference(), response);
        return response;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private Map<Long, ProductResponse> fetchProductMap(List<Long> productIds) {
        try {
            List<ProductResponse> products = productServiceClient.getProductsByIds(productIds);
            return products.stream()
                    .collect(Collectors.toMap(ProductResponse::getId, p -> p));
        } catch (ExternalServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ExternalServiceException("product-service", ex);
        }
    }

    private void validateProducts(List<CreateOrderRequest.OrderItemRequest> items,
                                  Map<Long, ProductResponse> productMap) {
        for (var item : items) {
            ProductResponse product = productMap.get(item.getProductId());
            if (product == null || !product.isActive()) {
                throw new ProductUnavailableException(item.getProductId());
            }
        }
    }

    private List<InventoryCheckResponse> checkInventoryBatch(
            List<CreateOrderRequest.OrderItemRequest> items) {
        List<InventoryCheckRequest> requests = items.stream()
                .map(i -> new InventoryCheckRequest(i.getProductId(), i.getQuantity()))
                .collect(Collectors.toList());
        try {
            return inventoryServiceClient.checkInventoryBatch(requests);
        } catch (ExternalServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ExternalServiceException("inventory-service", ex);
        }
    }

    private void validateStock(List<CreateOrderRequest.OrderItemRequest> items,
                               Map<Long, InventoryCheckResponse> stockMap) {
        for (var item : items) {
            InventoryCheckResponse stock = stockMap.get(item.getProductId());
            if (stock == null || !stock.isInStock()
                    || stock.getAvailableQuantity() < item.getQuantity()) {
                int available = (stock != null) ? stock.getAvailableQuantity() : 0;
                throw new InsufficientInventoryException(
                        item.getProductId(), item.getQuantity(), available);
            }
        }
    }

    private Order buildOrder(CreateOrderRequest request,
                             Map<Long, ProductResponse> productMap,
                             Map<Long, InventoryCheckResponse> stockMap) {

        Order order = Order.builder()
                .customerId(request.getCustomerId())
                .customerEmail(request.getCustomerEmail())
                .customerName(request.getCustomerName())
                .currencyCode(request.getCurrencyCode())
                .paymentMethod(request.getPaymentMethod())
                .notes(request.getNotes())
                .discountAmount(request.getDiscountAmount() != null
                        ? request.getDiscountAmount() : BigDecimal.ZERO)
                .shippingAddress(mapShippingAddress(request.getShippingAddress()))
                .status(OrderStatus.PENDING)
                .subtotal(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .shippingCost(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal totalTax = BigDecimal.ZERO;

        for (var itemReq : request.getItems()) {
            ProductResponse product = productMap.get(itemReq.getProductId());
            InventoryCheckResponse stock = stockMap.get(itemReq.getProductId());

            BigDecimal unitDiscount = itemReq.getUnitDiscount() != null
                    ? itemReq.getUnitDiscount() : BigDecimal.ZERO;

            OrderItem orderItem = OrderItem.builder()
                    .productId(product.getId())
                    .productSku(product.getSku())
                    .productName(product.getName())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(product.getPrice())
                    .unitDiscount(unitDiscount)
                    .warehouseId(stock.getWarehouseId())
                    .build();

            orderItem.calculateLineTotal();
            order.addOrderItem(orderItem);

            // Accumulate tax per item
            if (product.getTaxRate() != null) {
                BigDecimal itemTax = orderItem.getLineTotal()
                        .multiply(product.getTaxRate())
                        .setScale(4, RoundingMode.HALF_UP);
                totalTax = totalTax.add(itemTax);
            }
        }

        order.setTaxAmount(totalTax);
        order.recalculateTotal();
        return order;
    }

    private ShippingAddress mapShippingAddress(CreateOrderRequest.ShippingAddressRequest req) {
        if (req == null) return null;
        return ShippingAddress.builder()
                .street(req.getStreet()).city(req.getCity())
                .state(req.getState()).postalCode(req.getPostalCode())
                .country(req.getCountry()).phone(req.getPhone())
                .build();
    }

    private void reserveInventoryForOrder(Order order,
                                          Map<Long, InventoryCheckResponse> stockMap) {
        for (OrderItem item : order.getOrderItems()) {
            InventoryCheckResponse stock = stockMap.get(item.getProductId());
            inventoryServiceClient.reserveInventory(
                    new InventoryReserveRequest(
                            order.getOrderReference(),
                            item.getProductId(),
                            item.getQuantity(),
                            stock != null ? stock.getWarehouseId() : null
                    )
            );
        }
    }

    private void releaseInventoryForOrder(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            try {
                inventoryServiceClient.releaseInventory(
                        new InventoryReleaseRequest(
                                order.getOrderReference(),
                                item.getProductId(),
                                item.getQuantity()
                        )
                );
            } catch (Exception ex) {
                // Non-fatal: log and schedule retry via outbox / dead-letter
                log.error("Failed to release inventory for product {} on order {}",
                        item.getProductId(), order.getOrderReference(), ex);
            }
        }
    }

    private void applyStatusSideEffects(Order order, UpdateOrderStatusRequest request) {
        LocalDateTime now = LocalDateTime.now();
        switch (request.getStatus()) {
            case CONFIRMED -> {
                order.setConfirmedAt(now);
                if (request.getPaymentTransactionId() != null)
                    order.setPaymentTransactionId(request.getPaymentTransactionId());
            }
            case SHIPPED -> order.setShippedAt(now);
            case DELIVERED -> order.setDeliveredAt(now);
            case CANCELLED,
                 FAILED -> order.setCancelledAt(now);
            default -> { /* no extra side effect */ }
        }
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            order.setNotes(request.getNotes());
        }
    }

    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        boolean valid = switch (current) {
            case PENDING -> next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED
                    || next == OrderStatus.FAILED;
            case CONFIRMED -> next == OrderStatus.PROCESSING || next == OrderStatus.CANCELLED;
            case PROCESSING -> next == OrderStatus.SHIPPED || next == OrderStatus.CANCELLED
                    || next == OrderStatus.FAILED;
            case SHIPPED -> next == OrderStatus.DELIVERED || next == OrderStatus.FAILED;
            case DELIVERED -> next == OrderStatus.REFUNDED;
            default -> false;  // terminal states
        };
        if (!valid) {
            throw new InvalidOrderStatusTransitionException(current.name(), next.name());
        }
    }

    private PagedOrderResponse toPagedResponse(Page<Order> page, Pageable pageable) {
        List<OrderResponse> responses = page.getContent()
                .stream().map(OrderResponse::from).collect(Collectors.toList());
        return PagedOrderResponse.builder()
                .orders(responses)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    /**
     * Manual cache put – used after create and refresh.
     */
    private void putInCache(Long id, String reference, OrderResponse response) {
        try {
            String keyById = RedisConfig.CACHE_ORDER_BY_ID + "::" + id;
            String keyByRef = RedisConfig.CACHE_ORDER_BY_REFERENCE + "::" + reference;
            redisTemplate.opsForValue().set(keyById, response);
            redisTemplate.opsForValue().set(keyByRef, response);
        } catch (Exception ex) {
            // Cache is non-critical; log and continue
            log.warn("Failed to populate cache for order id={}: {}", id, ex.getMessage());
        }
    }

    private void evictReferenceCache(String reference) {
        try {
            redisTemplate.delete(RedisConfig.CACHE_ORDER_BY_REFERENCE + "::" + reference);
        } catch (Exception ex) {
            log.warn("Failed to evict cache for reference {}: {}", reference, ex.getMessage());
        }
    }