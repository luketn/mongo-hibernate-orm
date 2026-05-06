package com.luketn.crystalshop.domain.api;

public record InventoryItemRequest(
        Long storeId,
        Long crystalId,
        Integer quantity,
        String shelfLocation
) {
}
