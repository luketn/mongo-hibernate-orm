package com.luketn.crystalshop.domain.api;

import com.luketn.crystalshop.domain.database.Customer;

public record CustomerView(
        String id,
        String name,
        String email,
        String loyaltyTier
) {
    public static CustomerView from(Customer customer) {
        return new CustomerView(
                ApiIds.toString(customer.getId()),
                customer.getName(),
                customer.getEmail(),
                customer.getLoyaltyTier()
        );
    }
}
