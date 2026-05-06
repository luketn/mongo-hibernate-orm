package com.luketn.crystalshop.domain.api;

import java.math.BigDecimal;
import java.util.List;

public record SaleView(
        Long id,
        Long storeId,
        String storeCode,
        String storeName,
        Long customerId,
        String customerEmail,
        String customerName,
        String soldAt,
        List<SaleLineView> lines,
        List<String> lineSummary,
        BigDecimal total
) {
}
