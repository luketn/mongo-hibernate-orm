package com.luketn.crystalshop.domain.api;

import java.math.BigDecimal;

public record CrystalRequest(
        String sku,
        String name,
        String family,
        String color,
        String origin,
        BigDecimal retailPrice
) {
}
