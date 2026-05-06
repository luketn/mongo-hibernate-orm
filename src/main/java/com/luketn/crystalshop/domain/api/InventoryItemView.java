package com.luketn.crystalshop.domain.api;

public record InventoryItemView(
        Long id,
        Long storeId,
        String storeCode,
        String storeName,
        Long crystalId,
        String crystalSku,
        String crystalName,
        int quantity,
        String shelfLocation
) {
}
