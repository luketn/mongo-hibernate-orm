package com.luketn.crystalshop;

import com.mongodb.client.model.Filters;
import com.luketn.crystalshop.domain.database.Crystal;
import com.luketn.crystalshop.domain.database.Customer;
import com.luketn.crystalshop.domain.database.InventoryItem;
import com.luketn.crystalshop.domain.database.Sale;
import com.luketn.crystalshop.domain.database.SaleLine;
import com.luketn.crystalshop.domain.database.Store;
import com.luketn.crystalshop.persistence.HibernateSupport;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class MongoEntityMappingTest {
    @Container
    static final MongoDBContainer mongo = new MongoDBContainer(MongoTestSupport.MONGO_IMAGE);

    @Test
    void persistsObjectIdEntitiesInstantSaleTimestampsAndEmbeddedSaleLines() {
        Object saleId;
        try (SessionFactory sessionFactory = HibernateSupport.createSessionFactory(
                MongoTestSupport.mongoConfig(mongo, "none", 0)
        )) {
            try (var session = sessionFactory.openSession()) {
                var transaction = session.beginTransaction();

                Crystal crystal = new Crystal(
                        "TST-001",
                        "Test Amethyst",
                        "Quartz",
                        "Violet",
                        "Uruguay",
                        new BigDecimal("40.00"),
                        new BigDecimal("15.00")
                );
                Customer customer = new Customer("Test Customer", "test.customer@example.com", "GOLD");
                Store store = new Store("TST-STORE", "Test Store", "Sydney", "1 Test Street");
                session.persist(crystal);
                session.persist(customer);
                session.persist(store);
                session.flush();

                assertInstanceOf(ObjectId.class, crystal.getId());
                assertInstanceOf(ObjectId.class, customer.getId());
                assertInstanceOf(ObjectId.class, store.getId());

                InventoryItem item = new InventoryItem(store, crystal, 4, "A1");
                Sale sale = new Sale(store, customer, LocalDateTime.parse("2026-04-23T09:00:00"));
                sale.addLine(new SaleLine(crystal, 2, new BigDecimal("40.00")));
                sale.addLine(new SaleLine(crystal, 1, new BigDecimal("38.00")));
                session.persist(item);
                session.persist(sale);
                session.flush();

                assertInstanceOf(ObjectId.class, item.getId());
                assertInstanceOf(ObjectId.class, sale.getId());
                assertInstanceOf(Instant.class, sale.getSoldAt());
                assertEquals(2, sale.getLines().size());
                saleId = sale.getId();

                transaction.commit();
            }
        }

        try (var client = MongoTestSupport.mongoClient(mongo)) {
            var database = client.getDatabase(MongoTestSupport.databaseName(mongo));
            Document sale = database.getCollection("sales")
                    .find(Filters.eq("_id", saleId))
                    .first();
            assertNotNull(sale);
            assertInstanceOf(List.class, sale.get("lines"));
            assertEquals(2, sale.getList("lines", Document.class).size());
            assertNotNull(sale.get("soldAt"));
            assertFalse(database.listCollectionNames().into(new java.util.ArrayList<>()).contains("sale_lines"));
        }
    }
}
