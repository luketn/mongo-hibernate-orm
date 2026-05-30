package com.luketn.crystalshop.domain.api;

import com.luketn.crystalshop.domain.database.SaleLine;

import java.math.BigDecimal;

public record SaleLineView(
        String id,
        String crystalId,
        String crystalSku,
        String crystalName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
    public static SaleLineView from(SaleLine line) {
        return new SaleLineView(
                ApiIds.toString(line.getId()),
                ApiIds.toString(line.getCrystalId()),
                line.getCrystalSku(),
                line.getCrystalName(),
                line.getQuantity(),
                line.getUnitPrice(),
                line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity()))
        );
    }
}
