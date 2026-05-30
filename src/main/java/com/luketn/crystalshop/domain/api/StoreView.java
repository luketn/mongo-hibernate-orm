package com.luketn.crystalshop.domain.api;

import com.luketn.crystalshop.domain.database.Store;

public record StoreView(
        String id,
        String code,
        String name,
        String city,
        String address
) {
    public static StoreView from(Store store) {
        return new StoreView(
                ApiIds.toString(store.getId()),
                store.getCode(),
                store.getName(),
                store.getCity(),
                store.getAddress()
        );
    }
}
