package com.luketn.crystalshop.domain.api;

import com.luketn.crystalshop.domain.database.Crystal;

import java.math.BigDecimal;

public record CrystalView(
        String id,
        String sku,
        String name,
        String family,
        String color,
        String origin,
        BigDecimal retailPrice,
        BigDecimal wholesaleCost
) {
    public static CrystalView from(Crystal crystal) {
        return new CrystalView(
                ApiIds.toString(crystal.getId()),
                crystal.getSku(),
                crystal.getName(),
                crystal.getFamily(),
                crystal.getColor(),
                crystal.getOrigin(),
                crystal.getRetailPrice(),
                crystal.getWholesaleCost()
        );
    }
}
