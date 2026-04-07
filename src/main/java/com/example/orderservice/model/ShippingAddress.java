package com.example.orderservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingAddress implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "shipping_street",      length = 255)
    private String street;

    @Column(name = "shipping_city",        length = 100)
    private String city;

    @Column(name = "shipping_state",       length = 100)
    private String state;

    @Column(name = "shipping_postal_code", length = 20)
    private String postalCode;

    @Column(name = "shipping_country",     length = 100)
    private String country;

    @Column(name = "shipping_phone",       length = 30)
    private String phone;
}