package com.luketn.crystalshop.domain.api;

import java.util.List;

public record SaleRequest(
        Long storeId,
        Long customerId,
        String soldAt,
        List<SaleLineRequest> lines
) {
}
