package com.luketn.crystalshop.domain.api;

import com.luketn.crystalshop.domain.database.InventoryItem;

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
    public static InventoryItemView from(InventoryItem item) {
        return new InventoryItemView(
                item.getId(),
                item.getStore().getId(),
                item.getStore().getCode(),
                item.getStore().getName(),
                item.getCrystal().getId(),
                item.getCrystal().getSku(),
                item.getCrystal().getName(),
                item.getQuantity(),
                item.getShelfLocation()
        );
    }
}
