package com.luketn.crystalshop;

import com.luketn.crystalshop.domain.api.AnnualSalesReport;
import com.luketn.crystalshop.domain.api.ReportTotals;
import com.luketn.crystalshop.persistence.HibernateSupport;
import com.luketn.crystalshop.service.CrystalShopReportingService;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class MongoReportingServiceTest {
    @Container
    static final MongoDBContainer mongo = new MongoDBContainer(MongoTestSupport.MONGO_IMAGE);

    @BeforeEach
    void clearDatabase() {
        try (var client = MongoTestSupport.mongoClient(mongo)) {
            client.getDatabase(MongoTestSupport.databaseName(mongo)).drop();
        }
    }

    @Test
    void annualSalesReportUsesMongoAggregationsForTheSampleFixture() {
        try (SessionFactory sessionFactory = HibernateSupport.createSessionFactory(
                MongoTestSupport.mongoConfig(mongo, "none", 0)
        )) {
            new SampleDataImporter(sessionFactory).importSampleData();
            AnnualSalesReport report = new CrystalShopReportingService(mongo.getReplicaSetUrl()).annualSalesReport(2025);

            assertEquals(2025, report.year());
            assertEquals(2026, report.forecastYear());
            assertTrue(report.totals().revenue().compareTo(BigDecimal.ZERO) > 0);
            assertTrue(report.totals().profit().compareTo(BigDecimal.ZERO) > 0);
            assertTrue(report.totals().costs().compareTo(BigDecimal.ZERO) > 0);
            assertTrue(report.weeklySalesTrends().size() > 20);
            assertEquals(12, report.monthlyCustomerRetention().size());
            assertFalse(report.bestSellingProducts().isEmpty());
            assertFalse(report.forecasts().isEmpty());
            assertTrue(report.recommendations().size() >= 3);
        }
    }

    @Test
    void reportTotalsRecordCodecPreservesTwoDecimalMoneyScale() {
        ReportTotals original = new ReportTotals(
                new BigDecimal("123.40"),
                new BigDecimal("45.60"),
                new BigDecimal("77.80"),
                9,
                3,
                2
        );

        try (var client = MongoTestSupport.mongoClient(mongo)) {
            var collection = client.getDatabase(MongoTestSupport.databaseName(mongo))
                    .getCollection("reportTotalsCodec", ReportTotals.class);
            collection.insertOne(original);

            ReportTotals stored = collection.find().first();

            assertEquals(original, stored);
            assertTwoDecimalScale(stored.revenue());
            assertTwoDecimalScale(stored.profit());
            assertTwoDecimalScale(stored.costs());
        }
    }

    @Test
    void annualSalesReportPreservesTwoDecimalMoneyScaleFromAggregation() {
        try (SessionFactory sessionFactory = HibernateSupport.createSessionFactory(
                MongoTestSupport.mongoConfig(mongo, "none", 0)
        )) {
            new SampleDataImporter(sessionFactory).importSampleData();
            AnnualSalesReport report = new CrystalShopReportingService(mongo.getReplicaSetUrl()).annualSalesReport(2025);

            assertTwoDecimalScale(report.totals().revenue());
            assertTwoDecimalScale(report.totals().profit());
            assertTwoDecimalScale(report.totals().costs());
        }
    }

    @Test
    void annualSalesReportUsesZeroTotalsWhenNoSalesMatch() {
        AnnualSalesReport report = new CrystalShopReportingService(mongo.getReplicaSetUrl()).annualSalesReport(2025);

        assertSame(ReportTotals.ZERO, report.totals());
        assertTwoDecimalScale(report.totals().revenue());
        assertTwoDecimalScale(report.totals().profit());
        assertTwoDecimalScale(report.totals().costs());
    }

    private void assertTwoDecimalScale(BigDecimal value) {
        assertNotNull(value);
        assertEquals(2, value.scale(), value.toPlainString());
    }
}
