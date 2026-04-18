package com.example.orderservice.client;

import com.example.orderservice.dto.InventoryCheckRequest;
import com.example.orderservice.dto.InventoryCheckResponse;
import com.example.orderservice.dto.InventoryReleaseRequest;
import com.example.orderservice.dto.InventoryReserveRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(
        name = "inventory-service",
        url = "${services.inventory.url}",
        fallbackFactory = InventoryServiceFallbackFactory.class
)
public interface InventoryServiceClient {

    /**
     * Check stock availability without reserving.
     */
    @PostMapping("/api/v1/inventory/check")
    InventoryCheckResponse checkInventory(@RequestBody InventoryCheckRequest request);

    /**
     * Reserve (soft-lock) stock during order processing.
     */
    @PostMapping("/api/v1/inventory/reserve")
    void reserveInventory(@RequestBody InventoryReserveRequest request);

    /**
     * Release reservation on cancellation / failure.
     */
    @PostMapping("/api/v1/inventory/release")
    void releaseInventory(@RequestBody InventoryReleaseRequest request);

    /**
     * Batch-check for multiple products.
     */
    @PostMapping("/api/v1/inventory/check/batch")
    List<InventoryCheckResponse> checkInventoryBatch(@RequestBody List<InventoryCheckRequest> requests);
}