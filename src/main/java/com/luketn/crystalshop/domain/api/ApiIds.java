package com.luketn.crystalshop.domain.api;

final class ApiIds {
    private ApiIds() {
    }

    static String toString(Object id) {
        return id == null ? null : id.toString();
    }
}
