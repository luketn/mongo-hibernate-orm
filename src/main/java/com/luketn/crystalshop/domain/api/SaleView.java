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
                .map(line -> line.getCrystalSku() + " x" + line.getQuantity())
                .toList();
        BigDecimal total = sale.getLines().stream()
                .map(line -> line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new SaleView(
                ApiIds.toString(sale.getId()),
                ApiIds.toString(sale.getStoreId()),
                sale.getStore() == null ? null : sale.getStore().getCode(),
                sale.getStore() == null ? null : sale.getStore().getName(),
                ApiIds.toString(sale.getCustomerId()),
                sale.getCustomer() == null ? null : sale.getCustomer().getEmail(),
                sale.getCustomer() == null ? null : sale.getCustomer().getName(),
                sale.getSoldAt().toString(),
                lines,
                lineSummary,
                total
        );
    }

    public static SaleView from(Sale sale, StoreView store, CustomerView customer) {
        List<SaleLineView> lines = sale.getLines().stream().map(SaleLineView::from).toList();
        List<String> lineSummary = sale.getLines().stream()
                .map(line -> line.getCrystalSku() + " x" + line.getQuantity())
                .toList();
        BigDecimal total = sale.getLines().stream()
                .map(line -> line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new SaleView(
                ApiIds.toString(sale.getId()),
                ApiIds.toString(sale.getStoreId()),
                store.code(),
                store.name(),
                ApiIds.toString(sale.getCustomerId()),
                customer.email(),
                customer.name(),
                sale.getSoldAt().toString(),
                lines,
                lineSummary,
                total
        );
    }
}
