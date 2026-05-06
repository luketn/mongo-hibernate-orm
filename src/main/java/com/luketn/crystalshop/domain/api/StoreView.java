package com.luketn.crystalshop.domain.api;

public record StoreView(
        Long id,
        String code,
        String name,
        String city,
        String address
) {
}
