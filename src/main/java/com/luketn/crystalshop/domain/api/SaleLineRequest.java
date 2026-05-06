package com.luketn.crystalshop.domain.api;

import java.math.BigDecimal;

public record SaleLineRequest(
        Long crystalId,
        Integer quantity,
        BigDecimal unitPrice
) {
}
