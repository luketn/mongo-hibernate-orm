package com.luketn.crystalshop.domain.api;

import java.math.BigDecimal;

public record ProductForecast(
        String crystalSku,
        String crystalName,
        BigDecimal projectedRevenue,
        int projectedUnits,
        BigDecimal growthRate
) {
}
