package com.luketn.crystalshop.domain.api;

public record CustomerView(
        Long id,
        String name,
        String email,
        String loyaltyTier
) {
}
