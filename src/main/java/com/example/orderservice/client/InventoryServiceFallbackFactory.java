package com.example.orderservice.client;

import com.example.orderservice.dto.InventoryCheckRequest;
import com.example.orderservice.dto.InventoryCheckResponse;
import com.example.orderservice.dto.InventoryReleaseRequest;
import com.example.orderservice.dto.InventoryReserveRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Fallback factory for InventoryServiceClient.
 * Returns safe defaults when the InventoryService is unavailable.
 */
@Slf4j
@Component
public class InventoryServiceFallbackFactory implements FallbackFactory<InventoryServiceClient> {

    @Override
    public InventoryServiceClient create(Throwable cause) {
        log.warn("InventoryService is unavailable, using fallback", cause);
        return new InventoryServiceFallback();
    }

    /**
     * Fallback implementation for InventoryServiceClient.
     */
    @Slf4j
    private static class InventoryServiceFallback implements InventoryServiceClient {

        @Override
        public InventoryCheckResponse checkInventory(InventoryCheckRequest request) {
            log.debug("Fallback: returning no-stock response for productId={}", request.getProductId());
            return InventoryCheckResponse.builder()
                    .productId(request.getProductId())
                    .inStock(false)
                    .availableQuantity(0)
                    .warehouseId(null)
                    .build();
        }

        @Override
        public void reserveInventory(InventoryReserveRequest request) {
            log.debug("Fallback: cannot reserve inventory for productId={} on order={}",
                    request.getProductId(), request.getOrderReference());
        }

        @Override
        public void releaseInventory(InventoryReleaseRequest request) {
            log.debug("Fallback: cannot release inventory for productId={} on order={}",
                    request.getProductId(), request.getOrderReference());
        }

        @Override
        public List<InventoryCheckResponse> checkInventoryBatch(List<InventoryCheckRequest> requests) {
            log.debug("Fallback: returning empty list for batch inventory check (requested {} items)",
                    requests.size());
            return Collections.emptyList();
        }
    }
}

