package com.luketn.crystalshop.domain.api;

import com.luketn.crystalshop.domain.database.Sale;

import java.math.BigDecimal;
import java.util.List;

public record SaleView(
        String id,
        String storeId,
        String storeCode,
        String storeName,
        String customerId,
        String customerEmail,
        String customerName,
        String soldAt,
        List<SaleLineView> lines,
        List<String> lineSummary,
        BigDecimal total
) {
    public static SaleView from(Sale sale) {
        List<SaleLineView> lines = sale.getLines().stream().map(SaleLineView::from).toList();
        List<String> lineSummary = sale.getLines().stream()
                .map(line -> line.getCrystal().getSku() + " x" + line.getQuantity())
                .toList();
        BigDecimal total = sale.getLines().stream()
                .map(line -> line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new SaleView(
                ApiIds.toString(sale.getId()),
                ApiIds.toString(sale.getStore().getId()),
                sale.getStore().getCode(),
                sale.getStore().getName(),
                ApiIds.toString(sale.getCustomer().getId()),
                sale.getCustomer().getEmail(),
                sale.getCustomer().getName(),
                sale.getSoldAt().toString(),
                lines,
                lineSummary,
                total
        );
    }
}
