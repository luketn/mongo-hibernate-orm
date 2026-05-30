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
                ApiIds.toString(item.getStoreId()),
                item.getStore() == null ? null : item.getStore().getCode(),
                item.getStore() == null ? null : item.getStore().getName(),
                ApiIds.toString(item.getCrystalId()),
                item.getCrystal() == null ? null : item.getCrystal().getSku(),
                item.getCrystal() == null ? null : item.getCrystal().getName(),
                item.getQuantity(),
                item.getShelfLocation()
        );
    }

    public static InventoryItemView from(InventoryItem item, StoreView store, CrystalView crystal) {
        return new InventoryItemView(
                ApiIds.toString(item.getId()),
                ApiIds.toString(item.getStoreId()),
                store.code(),
                store.name(),
                ApiIds.toString(item.getCrystalId()),
                crystal.sku(),
                crystal.name(),
                item.getQuantity(),
                item.getShelfLocation()
        );
    }
}
