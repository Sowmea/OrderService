package com.example.orderservice.dto;

import lombok.*;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagedOrderResponse implements Serializable {
    private List<OrderResponse> orders;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
