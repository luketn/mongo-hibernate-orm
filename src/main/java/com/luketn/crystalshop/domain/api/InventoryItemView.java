package com.luketn.crystalshop.domain.api;

import com.luketn.crystalshop.domain.database.InventoryItem;

public record InventoryItemView(
        String id,
        String storeId,
        String storeCode,
        String storeName,
        String crystalId,
        String crystalSku,
        String crystalName,
        int quantity,
        String shelfLocation
) {
    public static InventoryItemView from(InventoryItem item) {
        return new InventoryItemView(
                ApiIds.toString(item.getId()),
                ApiIds.toString(item.getStore().getId()),
                item.getStore().getCode(),
                item.getStore().getName(),
                ApiIds.toString(item.getCrystal().getId()),
                item.getCrystal().getSku(),
                item.getCrystal().getName(),
                item.getQuantity(),
                item.getShelfLocation()
        );
    }
}
