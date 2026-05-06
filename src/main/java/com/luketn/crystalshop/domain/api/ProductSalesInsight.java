package com.luketn.crystalshop.domain.api;

import java.math.BigDecimal;

public record ProductSalesInsight(
        String crystalSku,
        String crystalName,
        int unitsSold,
        BigDecimal revenue,
        BigDecimal profit,
        BigDecimal margin
) {
}
