package com.example.orderservice.client;

import com.example.orderservice.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

// ─── Product Service client ───────────────────────────────────────────────────

@FeignClient(
        name = "product-service",
        url = "${services.product.url}",
        fallbackFactory = ProductServiceFallbackFactory.class
)
public interface ProductServiceClient {

    @GetMapping("/api/v1/products/{id}")
    ProductResponse getProductById(@PathVariable("id") Long id);

    @PostMapping("/api/v1/products/batch")
    List<ProductResponse> getProductsByIds(@RequestBody List<Long> ids);
}
