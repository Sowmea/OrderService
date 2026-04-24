package com.example.orderservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.io.Serializable;

// ═══════════════════════════════════════════════════════════════════════════════
// REQUEST  DTOs
// ═══════════════════════════════════════════════════════════════════════════════
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingAddressRequest implements Serializable {
    @NotBlank(message = "Street is required")
    private String street;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Postal code is required")
    private String postalCode;

    @NotBlank(message = "Country is required")
    private String country;

    private String phone;
}
