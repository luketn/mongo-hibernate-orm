package com.luketn.crystalshop.domain.api;

import java.math.BigDecimal;

public record WeeklySalesTrend(
        String weekStart,
        String crystalSku,
        String crystalName,
        int unitsSold,
        BigDecimal revenue,
        BigDecimal profit
) {
}
