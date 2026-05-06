package com.luketn.crystalshop.domain.api;

public record StoreRequest(
        String code,
        String name,
        String city,
        String address
) {
}
