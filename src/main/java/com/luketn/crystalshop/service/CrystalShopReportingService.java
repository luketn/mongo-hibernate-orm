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
import com.mongodb.client.model.Field;
import com.mongodb.client.model.mql.MqlBoolean;
import com.mongodb.client.model.mql.MqlDate;
import com.mongodb.client.model.mql.MqlNumber;
import com.mongodb.client.model.mql.MqlString;
import com.mongodb.client.model.mql.MqlValue;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.mongodb.client.model.Accumulators.addToSet;
import static com.mongodb.client.model.Accumulators.sum;
import static com.mongodb.client.model.Aggregates.group;
import static com.mongodb.client.model.Aggregates.limit;
import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Aggregates.project;
import static com.mongodb.client.model.Aggregates.set;
import static com.mongodb.client.model.Aggregates.sort;
import static com.mongodb.client.model.Aggregates.unwind;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.lt;
import static com.mongodb.client.model.Projections.computed;
import static com.mongodb.client.model.Projections.excludeId;
import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;
import static com.mongodb.client.model.Sorts.ascending;
import static com.mongodb.client.model.Sorts.descending;
import static com.mongodb.client.model.Sorts.orderBy;
import static com.mongodb.client.model.mql.MqlValues.current;
import static com.mongodb.client.model.mql.MqlValues.of;

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
            ReportTotals totals = totals(database, window);
            List<WeeklySalesTrend> weeklySalesTrends = weeklySalesTrends(database, window);
            List<MonthlyCustomerRetention> monthlyCustomerRetention = monthlyCustomerRetention(database, window);
            List<ProductSalesInsight> bestSellingProducts = bestSellingProducts(database, window);
            List<ProductForecast> forecasts = forecasts(database, window);
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

    private ReportTotals totals(MongoDatabase database, ReportWindow window) {
        List<Document> rows = database.getCollection("sales")
                .aggregate(yearlyLinePipeline(
                        window,
                        group(null,
                                sum("revenue", number("lineRevenue")),
                                sum("profit", number("lineProfit")),
                                sum("costs", number("lineCost")),
                                sum("unitsSold", lineNumber("quantity")),
                                addToSet("saleIds", value("_id")),
                                addToSet("customerIds", value("customerId"))),
                        project(fields(
                                excludeId(),
                                include("revenue", "profit", "costs", "unitsSold"),
                                computed("salesCount", arraySize("saleIds")),
                                computed("activeCustomers", arraySize("customerIds"))))
                ))
                .into(new ArrayList<>());

        if (rows.isEmpty()) {
            return new ReportTotals(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0, 0);
        }
        Document row = rows.getFirst();
        return new ReportTotals(
                money(decimal(row.get("revenue"))),
                money(decimal(row.get("profit"))),
                money(decimal(row.get("costs"))),
                integer(row.get("unitsSold")),
                integer(row.get("salesCount")),
                integer(row.get("activeCustomers"))
        );
    }

    private List<WeeklySalesTrend> weeklySalesTrends(MongoDatabase database, ReportWindow window) {
        return database.getCollection("sales")
                .aggregate(yearlyLinePipeline(
                        window,
                        group(groupKey(
                                        new Field<>("weekStart", dateTrunc(date("soldAt"), "week")),
                                        new Field<>("crystalSku", lineString("crystalSku")),
                                        new Field<>("crystalName", lineString("crystalName"))),
                                sum("unitsSold", lineNumber("quantity")),
                                sum("revenue", number("lineRevenue")),
                                sum("profit", number("lineProfit"))),
                        sort(ascending("_id.weekStart", "_id.crystalSku")),
                        project(fields(
                                excludeId(),
                                computed("weekStart", date("_id", "weekStart").asString(of("UTC"), of("%Y-%m-%d"))),
                                computed("crystalSku", string("_id", "crystalSku")),
                                computed("crystalName", string("_id", "crystalName")),
                                include("unitsSold", "revenue", "profit")))
                ))
                .into(new ArrayList<>())
                .stream()
                .map(row -> new WeeklySalesTrend(
                        row.getString("weekStart"),
                        row.getString("crystalSku"),
                        row.getString("crystalName"),
                        integer(row.get("unitsSold")),
                        money(decimal(row.get("revenue"))),
                        money(decimal(row.get("profit")))
                ))
                .toList();
    }

    private List<MonthlyCustomerRetention> monthlyCustomerRetention(MongoDatabase database, ReportWindow window) {
        List<SaleActivity> monthlyActivity = saleActivity(database, window.retentionStart(), window.nextYear());
        List<SaleActivity> firstSeenActivity = saleActivity(database, Instant.EPOCH, window.nextYear());

        Map<YearMonth, Set<ObjectId>> activeByMonth = new HashMap<>();
        for (SaleActivity sale : monthlyActivity) {
            YearMonth month = YearMonth.from(sale.soldDate());
            activeByMonth.computeIfAbsent(month, ignored -> new HashSet<>()).add(sale.customerId());
        }

        Map<ObjectId, YearMonth> firstSeen = new HashMap<>();
        for (SaleActivity sale : firstSeenActivity) {
            YearMonth month = YearMonth.from(sale.soldDate());
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

    private List<ProductSalesInsight> bestSellingProducts(MongoDatabase database, ReportWindow window) {
        return database.getCollection("sales")
                .aggregate(productSummaryPipeline(
                        window,
                        set(new Field<>("margin", margin())),
                        sort(orderBy(descending("revenue"), descending("unitsSold"))),
                        limit(5),
                        project(fields(
                                excludeId(),
                                computed("crystalSku", string("_id", "crystalSku")),
                                computed("crystalName", string("_id", "crystalName")),
                                include("unitsSold", "revenue", "profit", "margin")))
                ))
                .into(new ArrayList<>())
                .stream()
                .map(row -> new ProductSalesInsight(
                        row.getString("crystalSku"),
                        row.getString("crystalName"),
                        integer(row.get("unitsSold")),
                        money(decimal(row.get("revenue"))),
                        money(decimal(row.get("profit"))),
                        decimal(row.get("margin")).setScale(4, RoundingMode.HALF_UP)
                ))
                .toList();
    }

    private List<ProductForecast> forecasts(MongoDatabase database, ReportWindow window) {
        return database.getCollection("sales")
                .aggregate(productSummaryPipeline(
                        window,
                        set(new Field<>("rawGrowthRate", rawGrowthRate())),
                        set(new Field<>("growthRate", growthRate())),
                        set(
                                new Field<>("projectedRevenue", projectedRevenue()),
                                new Field<>("projectedUnits", ceil(projectedUnits()))),
                        sort(descending("projectedRevenue")),
                        limit(5),
                        project(fields(
                                excludeId(),
                                computed("crystalSku", string("_id", "crystalSku")),
                                computed("crystalName", string("_id", "crystalName")),
                                include("projectedRevenue", "projectedUnits"),
                                computed("growthRate", number("growthRate").round(of(4)))))
                ))
                .into(new ArrayList<>())
                .stream()
                .map(row -> new ProductForecast(
                        row.getString("crystalSku"),
                        row.getString("crystalName"),
                        money(decimal(row.get("projectedRevenue"))),
                        integer(row.get("projectedUnits")),
                        decimal(row.get("growthRate")).setScale(4, RoundingMode.HALF_UP)
                ))
                .toList();
    }

    private List<Bson> yearlyLinePipeline(ReportWindow window, Bson... terminalStages) {
        List<Bson> pipeline = new ArrayList<>();
        pipeline.add(match(and(
                gte("soldAt", Date.from(window.start())),
                lt("soldAt", Date.from(window.nextYear()))
        )));
        pipeline.add(unwind("$lines"));
        pipeline.add(computedLineFields());
        pipeline.addAll(List.of(terminalStages));
        return pipeline;
    }

    private List<Bson> productSummaryPipeline(ReportWindow window, Bson... terminalStages) {
        List<Bson> pipeline = yearlyLinePipeline(
                window,
                set(new Field<>("saleMonth", date("soldAt").month(of("UTC")))),
                group(groupKey(
                                new Field<>("crystalSku", lineString("crystalSku")),
                                new Field<>("crystalName", lineString("crystalName"))),
                        sum("unitsSold", lineNumber("quantity")),
                        sum("revenue", number("lineRevenue")),
                        sum("profit", number("lineProfit")),
                        sum("firstHalfRevenue", when(number("saleMonth").lte(of(6)), number("lineRevenue"), of(0))),
                        sum("secondHalfRevenue", when(number("saleMonth").gt(of(6)), number("lineRevenue"), of(0))))
        );
        pipeline.addAll(List.of(terminalStages));
        return pipeline;
    }

    private Bson computedLineFields() {
        return set(
                new Field<>("lineRevenue", lineRevenue()),
                new Field<>("lineCost", lineCost()),
                new Field<>("lineProfit", lineProfit())
        );
    }

    private List<SaleActivity> saleActivity(MongoDatabase database, Instant start, Instant end) {
        List<Document> rows = database.getCollection("sales")
                .find(and(
                        gte("soldAt", Date.from(start)),
                        lt("soldAt", Date.from(end))
                ))
                .projection(include("customerId", "soldAt"))
                .into(new ArrayList<>());
        return rows.stream()
                .map(row -> new SaleActivity(row.getObjectId("customerId"), instant(row.get("soldAt"))))
                .toList();
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

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private Document groupKey(Field<?>... fields) {
        Document key = new Document();
        for (Field<?> field : fields) {
            key.append(field.getName(), field.getValue());
        }
        return key;
    }

    private MqlNumber margin() {
        return when(number("revenue").eq(of(0)), of(0), number("profit").divide(number("revenue")));
    }

    private MqlNumber rawGrowthRate() {
        return when(
                number("firstHalfRevenue").eq(of(0)),
                of(0.08),
                number("secondHalfRevenue").subtract(number("firstHalfRevenue")).divide(number("firstHalfRevenue"))
        );
    }

    private MqlNumber growthRate() {
        return number("rawGrowthRate").min(of(0.35)).max(of(-0.15));
    }

    private MqlNumber projectedRevenue() {
        return number("revenue").multiply(number("growthRate").add(1)).round(of(2));
    }

    private MqlNumber projectedUnits() {
        return number("unitsSold").multiply(number("growthRate").add(1));
    }

    private MqlNumber lineRevenue() {
        return lineNumber("quantity").multiply(lineNumber("unitPrice"));
    }

    private MqlNumber lineCost() {
        return lineNumber("quantity").multiply(lineNumber("wholesaleCostAtSale"));
    }

    private MqlNumber lineProfit() {
        return lineNumber("quantity")
                .multiply(lineNumber("unitPrice").subtract(lineNumber("wholesaleCostAtSale")));
    }

    private MqlNumber when(MqlBoolean condition, MqlNumber thenExpression, MqlNumber elseExpression) {
        return condition.cond(thenExpression, elseExpression);
    }

    private MqlNumber arraySize(String fieldName) {
        return current().<MqlValue>getArray(fieldName).size();
    }

    private MqlDate date(String fieldName) {
        return current().getDate(fieldName);
    }

    private MqlDate date(String documentField, String fieldName) {
        return current().getDocument(documentField).getDate(fieldName);
    }

    private MqlNumber number(String fieldName) {
        return current().getNumber(fieldName);
    }

    private MqlString string(String documentField, String fieldName) {
        return current().getDocument(documentField).getString(fieldName);
    }

    private MqlValue value(String fieldName) {
        return current().getField(fieldName);
    }

    private MqlNumber lineNumber(String fieldName) {
        return current().getDocument("lines").getNumber(fieldName);
    }

    private MqlString lineString(String fieldName) {
        return current().getDocument("lines").getString(fieldName);
    }

    private Document dateTrunc(MqlDate date, String unit) {
        return operator("$dateTrunc", new Document("date", date)
                .append("unit", unit)
                .append("startOfWeek", "Monday")
                .append("timezone", "UTC"));
    }

    private Document ceil(MqlNumber expression) {
        return operator("$ceil", expression);
    }

    private Document operator(String operator, Object expression) {
        return new Document(operator, expression);
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

    private int integer(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
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

        Instant retentionStart() {
            return LocalDate.of(year - 1, 12, 1).atStartOfDay().toInstant(ZoneOffset.UTC);
        }

        Instant nextYear() {
            return LocalDate.of(year + 1, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC);
        }
    }

    private record SaleActivity(ObjectId customerId, Instant soldAt) {
        LocalDate soldDate() {
            return soldAt.atZone(ZoneOffset.UTC).toLocalDate();
        }
    }
}
