package com.luketn.crystalshop.service;

import com.luketn.crystalshop.domain.api.AnnualSalesReport;
import com.luketn.crystalshop.domain.api.MonthlyCustomerRetention;
import com.luketn.crystalshop.domain.api.ProductForecast;
import com.luketn.crystalshop.domain.api.ProductSalesInsight;
import com.luketn.crystalshop.domain.api.ReportTotals;
import com.luketn.crystalshop.domain.api.WeeklySalesTrend;
import com.luketn.crystalshop.http.ApiException;
import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CrystalShopReportingService {
    private final String databaseUrl;

    public CrystalShopReportingService(String databaseUrl) {
        this.databaseUrl = databaseUrl;
    }

    public AnnualSalesReport annualSalesReport(int year) {
        if (year < 2000 || year > 2100) {
            throw new ApiException(400, "year must be between 2000 and 2100");
        }

        ConnectionString connectionString = new ConnectionString(databaseUrl);
        try (var client = MongoClients.create(connectionString)) {
            MongoDatabase database = client.getDatabase(databaseName(connectionString));
            ReportWindow window = new ReportWindow(year);
            List<ReportLine> lines = reportLines(database, window);
            List<SaleActivity> sales = saleActivity(database, window);
            ReportTotals totals = totals(lines);
            List<WeeklySalesTrend> weeklySalesTrends = weeklySalesTrends(lines);
            List<MonthlyCustomerRetention> monthlyCustomerRetention = monthlyCustomerRetention(sales, window);
            List<ProductSalesInsight> bestSellingProducts = bestSellingProducts(lines);
            List<ProductForecast> forecasts = forecasts(lines);
            return new AnnualSalesReport(
                    year,
                    year + 1,
                    totals,
                    weeklySalesTrends,
                    monthlyCustomerRetention,
                    bestSellingProducts,
                    forecasts,
                    recommendations(totals, monthlyCustomerRetention, bestSellingProducts, forecasts)
            );
        }
    }

    private List<ReportLine> reportLines(MongoDatabase database, ReportWindow window) {
        List<Document> rows = database.getCollection("sales")
                .aggregate(List.of(
                        Aggregates.match(Filters.and(
                                Filters.gte("soldAt", Date.from(window.start())),
                                Filters.lt("soldAt", Date.from(window.nextYear()))
                        )),
                        Aggregates.unwind("$lines"),
                        Aggregates.project(Projections.fields(
                                Projections.include("_id", "customerId", "soldAt"),
                                Projections.computed("crystalId", "$lines.crystalId"),
                                Projections.computed("crystalSku", "$lines.crystalSku"),
                                Projections.computed("crystalName", "$lines.crystalName"),
                                Projections.computed("quantity", "$lines.quantity"),
                                Projections.computed("unitPrice", "$lines.unitPrice"),
                                Projections.computed("wholesaleCostAtSale", "$lines.wholesaleCostAtSale")
                        ))
                ))
                .into(new ArrayList<>());

        return rows.stream().map(row -> {
            int quantity = number(row.get("quantity")).intValue();
            BigDecimal unitPrice = decimal(row.get("unitPrice"));
            BigDecimal wholesaleCost = decimal(row.get("wholesaleCostAtSale"));
            BigDecimal revenue = unitPrice.multiply(BigDecimal.valueOf(quantity));
            BigDecimal costs = wholesaleCost.multiply(BigDecimal.valueOf(quantity));
            return new ReportLine(
                    row.getObjectId("_id"),
                    row.getObjectId("customerId"),
                    instant(row.get("soldAt")),
                    row.getString("crystalSku"),
                    row.getString("crystalName"),
                    quantity,
                    money(revenue),
                    money(costs),
                    money(revenue.subtract(costs))
            );
        }).toList();
    }

    private List<SaleActivity> saleActivity(MongoDatabase database, ReportWindow window) {
        List<Document> rows = database.getCollection("sales")
                .find(Filters.lt("soldAt", Date.from(window.nextYear())))
                .into(new ArrayList<>());
        return rows.stream()
                .map(row -> new SaleActivity(
                        row.getObjectId("_id"),
                        row.getObjectId("customerId"),
                        instant(row.get("soldAt"))
                ))
                .toList();
    }

    private ReportTotals totals(List<ReportLine> lines) {
        BigDecimal revenue = lines.stream()
                .map(ReportLine::revenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal profit = lines.stream()
                .map(ReportLine::profit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal costs = lines.stream()
                .map(ReportLine::costs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int unitsSold = lines.stream().mapToInt(ReportLine::quantity).sum();
        int salesCount = (int) lines.stream().map(ReportLine::saleId).distinct().count();
        int activeCustomers = (int) lines.stream().map(ReportLine::customerId).distinct().count();
        return new ReportTotals(money(revenue), money(profit), money(costs), unitsSold, salesCount, activeCustomers);
    }

    private List<WeeklySalesTrend> weeklySalesTrends(List<ReportLine> lines) {
        Map<WeeklyProductKey, ProductAccumulator> weekly = new HashMap<>();
        for (ReportLine line : lines) {
            LocalDate weekStart = line.soldDate()
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            weekly.computeIfAbsent(
                            new WeeklyProductKey(weekStart, line.crystalSku(), line.crystalName()),
                            key -> new ProductAccumulator(line.crystalSku(), line.crystalName())
                    )
                    .add(line);
        }

        return weekly.entrySet().stream()
                .sorted(Map.Entry.<WeeklyProductKey, ProductAccumulator>comparingByKey())
                .map(entry -> new WeeklySalesTrend(
                        entry.getKey().weekStart().toString(),
                        entry.getValue().crystalSku(),
                        entry.getValue().crystalName(),
                        entry.getValue().units,
                        money(entry.getValue().revenue),
                        money(entry.getValue().profit)
                ))
                .toList();
    }

    private List<MonthlyCustomerRetention> monthlyCustomerRetention(List<SaleActivity> sales, ReportWindow window) {
        Map<YearMonth, Set<ObjectId>> activeByMonth = new HashMap<>();
        Map<ObjectId, YearMonth> firstSeen = new HashMap<>();
        for (SaleActivity sale : sales) {
            YearMonth month = YearMonth.from(sale.soldDate());
            activeByMonth.computeIfAbsent(month, ignored -> new HashSet<>()).add(sale.customerId());
            firstSeen.merge(sale.customerId(), month, (left, right) -> left.isBefore(right) ? left : right);
        }

        List<MonthlyCustomerRetention> retention = new ArrayList<>();
        for (int monthNumber = 1; monthNumber <= 12; monthNumber++) {
            YearMonth month = YearMonth.of(window.year(), monthNumber);
            YearMonth previousMonth = month.minusMonths(1);
            Set<ObjectId> active = activeByMonth.getOrDefault(month, Set.of());
            Set<ObjectId> previousActive = activeByMonth.getOrDefault(previousMonth, Set.of());
            int gained = (int) firstSeen.values().stream()
                    .filter(firstMonth -> firstMonth.equals(month))
                    .count();
            int lost = (int) previousActive.stream()
                    .filter(customerId -> !active.contains(customerId))
                    .count();
            retention.add(new MonthlyCustomerRetention(month.toString(), gained, lost, active.size()));
        }
        return retention;
    }

    private List<ProductSalesInsight> bestSellingProducts(List<ReportLine> lines) {
        return productAccumulators(lines).values().stream()
                .sorted(Comparator.comparing(ProductAccumulator::revenue).reversed()
                        .thenComparing(Comparator.comparingInt(ProductAccumulator::units).reversed()))
                .limit(5)
                .map(product -> new ProductSalesInsight(
                        product.crystalSku(),
                        product.crystalName(),
                        product.units,
                        money(product.revenue),
                        money(product.profit),
                        ratio(product.profit, product.revenue)
                ))
                .toList();
    }

    private List<ProductForecast> forecasts(List<ReportLine> lines) {
        return productAccumulators(lines).values().stream()
                .map(product -> {
                    BigDecimal growthRate = growthRate(product.firstHalfRevenue, product.secondHalfRevenue);
                    BigDecimal projectedRevenue = money(product.revenue.multiply(BigDecimal.ONE.add(growthRate)));
                    int projectedUnits = product.revenue.signum() == 0
                            ? 0
                            : BigDecimal.valueOf(product.units)
                            .multiply(BigDecimal.ONE.add(growthRate))
                            .setScale(0, RoundingMode.CEILING)
                            .intValue();
                    return new ProductForecast(
                            product.crystalSku(),
                            product.crystalName(),
                            projectedRevenue,
                            projectedUnits,
                            growthRate.setScale(4, RoundingMode.HALF_UP)
                    );
                })
                .sorted(Comparator.comparing(ProductForecast::projectedRevenue).reversed())
                .limit(5)
                .toList();
    }

    private Map<String, ProductAccumulator> productAccumulators(List<ReportLine> lines) {
        Map<String, ProductAccumulator> products = new LinkedHashMap<>();
        for (ReportLine line : lines) {
            products.computeIfAbsent(
                            line.crystalSku(),
                            sku -> new ProductAccumulator(line.crystalSku(), line.crystalName())
                    )
                    .add(line);
        }
        return products;
    }

    private BigDecimal growthRate(BigDecimal firstHalfRevenue, BigDecimal secondHalfRevenue) {
        BigDecimal growth;
        if (firstHalfRevenue.signum() == 0) {
            growth = new BigDecimal("0.08");
        } else {
            growth = secondHalfRevenue.subtract(firstHalfRevenue)
                    .divide(firstHalfRevenue, 6, RoundingMode.HALF_UP);
        }
        if (growth.compareTo(new BigDecimal("-0.15")) < 0) {
            return new BigDecimal("-0.15");
        }
        if (growth.compareTo(new BigDecimal("0.35")) > 0) {
            return new BigDecimal("0.35");
        }
        return growth;
    }

    private List<String> recommendations(
            ReportTotals totals,
            List<MonthlyCustomerRetention> retention,
            List<ProductSalesInsight> bestSellingProducts,
            List<ProductForecast> forecasts
    ) {
        List<String> recommendations = new ArrayList<>();
        bestSellingProducts.stream().findFirst().ifPresent(product ->
                recommendations.add("Prioritise replenishment and front-of-store placement for "
                        + product.crystalSku() + " because it leads annual revenue."));
        forecasts.stream().findFirst().ifPresent(product ->
                recommendations.add("Increase 2026 purchasing cover for " + product.crystalSku()
                        + "; the forecast projects " + product.projectedUnits() + " units."));

        int gained = retention.stream().mapToInt(MonthlyCustomerRetention::customersGained).sum();
        int lost = retention.stream().mapToInt(MonthlyCustomerRetention::customersLost).sum();
        if (lost > gained) {
            recommendations.add("Run a win-back campaign for lapsed customers before peak gift-buying months.");
        } else {
            recommendations.add("Keep loyalty follow-ups active; customer gains are ahead of month-to-month losses.");
        }

        if (totals.revenue().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal margin = totals.profit()
                    .divide(totals.revenue(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            recommendations.add("Maintain the current cost discipline; annual gross margin is "
                    + margin.setScale(1, RoundingMode.HALF_UP) + "%.");
        }
        return recommendations;
    }

    private BigDecimal ratio(BigDecimal numerator, BigDecimal denominator) {
        if (denominator.signum() == 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return numerator.divide(denominator, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal decimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Decimal128 decimal) {
            return decimal.bigDecimalValue();
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(value.toString());
    }

    private Number number(Object value) {
        if (value instanceof Number number) {
            return number;
        }
        return Integer.parseInt(value.toString());
    }

    private Instant instant(Object value) {
        if (value instanceof Date date) {
            return date.toInstant();
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        return Instant.parse(value.toString());
    }

    private String databaseName(ConnectionString connectionString) {
        String database = connectionString.getDatabase();
        return database == null || database.isBlank() ? "test" : database;
    }

    private record ReportWindow(int year) {
        Instant start() {
            return LocalDate.of(year, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC);
        }

        Instant nextYear() {
            return LocalDate.of(year + 1, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC);
        }
    }

    private record SaleActivity(ObjectId saleId, ObjectId customerId, Instant soldAt) {
        LocalDate soldDate() {
            return soldAt.atZone(ZoneOffset.UTC).toLocalDate();
        }
    }

    private record ReportLine(
            ObjectId saleId,
            ObjectId customerId,
            Instant soldAt,
            String crystalSku,
            String crystalName,
            int quantity,
            BigDecimal revenue,
            BigDecimal costs,
            BigDecimal profit
    ) {
        LocalDate soldDate() {
            return soldAt.atZone(ZoneOffset.UTC).toLocalDate();
        }
    }

    private record WeeklyProductKey(LocalDate weekStart, String crystalSku, String crystalName)
            implements Comparable<WeeklyProductKey> {
        @Override
        public int compareTo(WeeklyProductKey other) {
            int week = weekStart.compareTo(other.weekStart);
            if (week != 0) {
                return week;
            }
            return crystalSku.compareTo(other.crystalSku);
        }
    }

    private static final class ProductAccumulator {
        private final String crystalSku;
        private final String crystalName;
        private int units;
        private BigDecimal revenue = BigDecimal.ZERO;
        private BigDecimal profit = BigDecimal.ZERO;
        private BigDecimal firstHalfRevenue = BigDecimal.ZERO;
        private BigDecimal secondHalfRevenue = BigDecimal.ZERO;

        private ProductAccumulator(String crystalSku, String crystalName) {
            this.crystalSku = crystalSku;
            this.crystalName = crystalName;
        }

        private void add(ReportLine line) {
            units += line.quantity();
            revenue = revenue.add(line.revenue());
            profit = profit.add(line.profit());
            if (line.soldDate().getMonthValue() <= 6) {
                firstHalfRevenue = firstHalfRevenue.add(line.revenue());
            } else {
                secondHalfRevenue = secondHalfRevenue.add(line.revenue());
            }
        }

        private String crystalSku() {
            return crystalSku;
        }

        private String crystalName() {
            return crystalName;
        }

        private int units() {
            return units;
        }

        private BigDecimal revenue() {
            return revenue;
        }
    }
}
