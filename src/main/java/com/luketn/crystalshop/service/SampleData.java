package com.luketn.crystalshop.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

record SampleData(
        List<CrystalSeed> crystals,
        List<CustomerSeed> customers,
        List<StoreSeed> stores
) {
}

record CrystalSeed(
        String sku,
        String name,
        String family,
        String color,
        String origin,
        BigDecimal retailPrice
) {
}

record CustomerSeed(
        String name,
        String email,
        String loyaltyTier
) {
}

record StoreSeed(
        String code,
        String name,
        String city,
        String address,
        List<InventorySeed> inventory,
        List<SaleSeed> sales
) {
}

record InventorySeed(
        String crystalSku,
        int quantity,
        String shelfLocation
) {
}

record SaleSeed(
        String customerEmail,
        LocalDateTime soldAt,
        List<SaleLineSeed> lines
) {
}

record SaleLineSeed(
        String crystalSku,
        int quantity,
        BigDecimal unitPrice
) {
}
