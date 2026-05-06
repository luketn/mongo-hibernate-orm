package com.luketn.crystalshop.domain.api;

public record CustomerRequest(
        String name,
        String email,
        String loyaltyTier
) {
}
