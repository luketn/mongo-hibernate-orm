package com.luketn.crystalshop.domain.api;

import java.math.BigDecimal;

public record ReportTotals(
        BigDecimal revenue,
        BigDecimal profit,
        BigDecimal costs,
        int unitsSold,
        int salesCount,
        int activeCustomers
) {
}
