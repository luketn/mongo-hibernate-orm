package com.luketn.crystalshop.domain.api;

import java.math.BigDecimal;

public record SaleLineView(
        Long id,
        Long crystalId,
        String crystalSku,
        String crystalName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
}
