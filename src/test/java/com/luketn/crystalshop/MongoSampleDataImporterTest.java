package com.luketn.crystalshop;

import com.luketn.crystalshop.persistence.HibernateSupport;
import com.luketn.crystalshop.service.CrystalShopService;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class MongoSampleDataImporterTest {
    @Container
    static final MongoDBContainer mongo = new MongoDBContainer(MongoTestSupport.MONGO_IMAGE);

    @Test
    void importsSampleDataIntoMongoWithDisplayReferencesPreserved() {
        try (SessionFactory sessionFactory = HibernateSupport.createSessionFactory(
                MongoTestSupport.mongoConfig(mongo, "none", 0)
        )) {
            Map<String, Object> counts = new SampleDataImporter(sessionFactory).importSampleData();
            assertEquals(8, counts.get("crystals"));
            assertEquals(10, counts.get("customers"));
            assertEquals(3, counts.get("stores"));
            assertEquals(18, counts.get("inventoryItems"));
            assertEquals(39, counts.get("sales"));
            assertEquals(78, counts.get("saleLines"));

            CrystalShopService service = new CrystalShopService(sessionFactory);
            assertEquals(8, service.listCrystals().size());
            assertEquals(18, service.listInventory().size());
            assertEquals(39, service.listSales().size());
            assertTrue(service.listSales().toString().contains("SEL-003 x2"));
            assertTrue(service.listInventory().toString().contains("SYD-DAWN"));
        }
    }
}
