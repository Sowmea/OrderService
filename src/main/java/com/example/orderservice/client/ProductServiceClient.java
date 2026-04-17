package com.example.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ─── Product Service client ───────────────────────────────────────────────────

@FeignClient(
        name = "product-service",
        url  = "${services.product.url}",
        fallbackFactory = ProductServiceFallbackFactory.class
)
public interface ProductServiceClient {

    @GetMapping("/api/v1/products/{id}")
    ProductResponse getProductById(@PathVariable("id") Long id);

    @PostMapping("/api/v1/products/batch")
    List<ProductResponse> getProductsByIds(@RequestBody List<Long> ids);
}

// ─── Inventory Service client ─────────────────────────────────────────────────

@FeignClient(
        name = "inventory-service",
        url  = "${services.inventory.url}",
        fallbackFactory = InventoryServiceFallbackFactory.class
)
public interface InventoryServiceClient {

    /** Check stock availability without reserving. */
    @PostMapping("/api/v1/inventory/check")
    InventoryCheckResponse checkInventory(@RequestBody InventoryCheckRequest request);

    /** Reserve (soft-lock) stock during order processing. */
    @PostMapping("/api/v1/inventory/reserve")
    void reserveInventory(@RequestBody InventoryReserveRequest request);

    /** Release reservation on cancellation / failure. */
    @PostMapping("/api/v1/inventory/release")
    void releaseInventory(@RequestBody InventoryReleaseRequest request);

    /** Batch-check for multiple products. */
    @PostMapping("/api/v1/inventory/check/batch")
    List<InventoryCheckResponse> checkInventoryBatch(@RequestBody List<InventoryCheckRequest> requests);
}