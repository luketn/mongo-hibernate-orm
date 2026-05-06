package com.luketn.crystalshop.domain.api;

import java.util.List;

public record AnnualSalesReport(
        int year,
        int forecastYear,
        ReportTotals totals,
        List<WeeklySalesTrend> weeklySalesTrends,
        List<MonthlyCustomerRetention> monthlyCustomerRetention,
        List<ProductSalesInsight> bestSellingProducts,
        List<ProductForecast> forecasts,
        List<String> recommendations
) {
}
