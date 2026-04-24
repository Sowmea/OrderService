package com.example.orderservice.dto;

import com.example.orderservice.model.ShippingAddress;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingAddressResponse implements Serializable {
    private String street;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String phone;

    public static ShippingAddressResponse from(ShippingAddress addr) {
        if (addr == null) return null;
        return ShippingAddressResponse.builder()
                .street(addr.getStreet()).city(addr.getCity())
                .state(addr.getState()).postalCode(addr.getPostalCode())
                .country(addr.getCountry()).phone(addr.getPhone())
                .build();
    }
}

