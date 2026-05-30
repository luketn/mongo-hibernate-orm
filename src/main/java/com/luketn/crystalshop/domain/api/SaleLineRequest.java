package com.luketn.crystalshop.domain.api;

import java.math.BigDecimal;

public record SaleLineRequest(
        String crystalId,
        Integer quantity,
        BigDecimal unitPrice
) {
}
