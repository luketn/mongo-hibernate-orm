package com.luketn.crystalshop.domain.api;

import java.util.List;

public record SaleRequest(
        String storeId,
        String customerId,
        String soldAt,
        List<SaleLineRequest> lines
) {
}
