package com.luketn.crystalshop.domain.api;

public record MonthlyCustomerRetention(
        String month,
        int customersGained,
        int customersLost,
        int activeCustomers
) {
}
