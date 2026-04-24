package com.example.orderservice.client;

import com.example.orderservice.dto.ProductResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Fallback factory for ProductServiceClient.
 * Returns safe defaults when the ProductService is unavailable.
 */
@Slf4j
@Component
public class ProductServiceFallbackFactory implements FallbackFactory<ProductServiceClient> {

    @Override
    public ProductServiceClient create(Throwable cause) {
        log.warn("ProductService is unavailable, using fallback", cause);
        return new ProductServiceFallback();
    }

    /**
     * Fallback implementation for ProductServiceClient.
     */
    @Slf4j
    private static class ProductServiceFallback implements ProductServiceClient {

        @Override
        public ProductResponse getProductById(Long productId) {
            log.debug("Fallback: returning null for productId={}", productId);
            return null;
        }

        @Override
        public List<ProductResponse> getProductsByIds(List<Long> productIds) {
            log.debug("Fallback: returning empty list for {} products", productIds.size());
            return Collections.emptyList();
        }
    }
}

