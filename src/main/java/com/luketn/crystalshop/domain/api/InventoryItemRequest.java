package com.luketn.crystalshop.domain.api;

public record InventoryItemRequest(
        String storeId,
        String crystalId,
        Integer quantity,
        String shelfLocation
) {
}
