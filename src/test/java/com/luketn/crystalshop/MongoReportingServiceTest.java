package com.luketn.crystalshop;

import com.luketn.crystalshop.domain.api.AnnualSalesReport;
import com.luketn.crystalshop.persistence.HibernateSupport;
import com.luketn.crystalshop.service.CrystalShopReportingService;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class MongoReportingServiceTest {
    @Container
    static final MongoDBContainer mongo = new MongoDBContainer(MongoTestSupport.MONGO_IMAGE);

    @Test
    void annualSalesReportUsesMongoAggregationsForTheSampleFixture() {
        try (SessionFactory sessionFactory = HibernateSupport.createSessionFactory(
                MongoTestSupport.mongoConfig(mongo, "none", 0)
        )) {
            new SampleDataImporter(sessionFactory).importSampleData();
            AnnualSalesReport report = new CrystalShopReportingService(sessionFactory).annualSalesReport(2025);

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
}
