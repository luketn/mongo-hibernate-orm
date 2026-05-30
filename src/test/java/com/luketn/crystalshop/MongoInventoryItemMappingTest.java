package com.luketn.crystalshop;

import com.mongodb.client.model.Filters;
import com.luketn.crystalshop.domain.database.Crystal;
import com.luketn.crystalshop.domain.database.InventoryItem;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Testcontainers
class MongoInventoryItemMappingTest {
    @Container
    static final MongoDBContainer mongo = new MongoDBContainer(MongoTestSupport.MONGO_IMAGE);

    @Test
    void persistsInventoryItemsWithExplicitObjectIdReferences() {
        ObjectId itemId;
        ObjectId storeId;
        ObjectId crystalId;
        try (SessionFactory sessionFactory = HibernateSupport.createSessionFactory(
                MongoTestSupport.mongoConfig(mongo, "none", 0),
                Crystal.class,
                Store.class,
                InventoryItem.class
        )) {
            try (var session = sessionFactory.openSession()) {
                var transaction = session.beginTransaction();

                Crystal crystal = new Crystal(
                        "INV-001",
                        "Inventory Amethyst",
                        "Quartz",
                        "Violet",
                        "Uruguay",
                        new BigDecimal("40.00"),
                        new BigDecimal("15.00")
                );
                Store store = new Store("INV-STORE", "Inventory Store", "Sydney", "1 Test Street");
                session.persist(crystal);
                session.persist(store);
                session.flush();

                InventoryItem item = new InventoryItem(store, crystal, 7, "B4");
                session.persist(item);
                session.flush();

                itemId = item.getId();
                storeId = store.getId();
                crystalId = crystal.getId();
                assertInstanceOf(ObjectId.class, itemId);
                assertEquals(storeId, item.getStoreId());
                assertEquals(crystalId, item.getCrystalId());
                assertEquals("B4", session.find(InventoryItem.class, itemId).getShelfLocation());

                transaction.commit();
            }
        }

        try (var client = MongoTestSupport.mongoClient(mongo)) {
            Document item = client.getDatabase(MongoTestSupport.databaseName(mongo))
                    .getCollection("inventory")
                    .find(Filters.eq("_id", itemId))
                    .first();
            assertNotNull(item);
            assertEquals(storeId, item.get("storeId"));
            assertEquals(crystalId, item.get("crystalId"));
            assertNull(item.get("store"));
            assertNull(item.get("crystal"));
        }
    }
}
