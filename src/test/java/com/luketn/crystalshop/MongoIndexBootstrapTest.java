package com.luketn.crystalshop;

import com.luketn.crystalshop.persistence.MongoIndexBootstrap;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class MongoIndexBootstrapTest {
    @Container
    static final MongoDBContainer mongo = new MongoDBContainer(MongoTestSupport.MONGO_IMAGE);

    @Test
    void createsExpectedMongoIndexesThroughJavaDriver() {
        MongoIndexBootstrap.createIndexes(mongo.getReplicaSetUrl());

        try (var client = MongoTestSupport.mongoClient(mongo)) {
            var database = client.getDatabase(MongoTestSupport.databaseName(mongo));
            assertUniqueIndex(database.getCollection("crystals").listIndexes().into(new java.util.ArrayList<>()),
                    "uk_crystals_sku", new Document("sku", 1));
            assertUniqueIndex(database.getCollection("customers").listIndexes().into(new java.util.ArrayList<>()),
                    "uk_customers_email", new Document("email", 1));
            assertUniqueIndex(database.getCollection("stores").listIndexes().into(new java.util.ArrayList<>()),
                    "uk_stores_code", new Document("code", 1));
            assertUniqueIndex(database.getCollection("inventory").listIndexes().into(new java.util.ArrayList<>()),
                    "uk_inventory_store_crystal", new Document("storeId", 1).append("crystalId", 1));

            List<Document> inventoryIndexes = database.getCollection("inventory")
                    .listIndexes()
                    .into(new java.util.ArrayList<>());
            assertIndex(inventoryIndexes, "idx_inventory_store", new Document("storeId", 1));
            assertIndex(inventoryIndexes, "idx_inventory_crystal", new Document("crystalId", 1));

            List<Document> saleIndexes = database.getCollection("sales")
                    .listIndexes()
                    .into(new java.util.ArrayList<>());
            assertIndex(saleIndexes, "idx_sales_store", new Document("storeId", 1));
            assertIndex(saleIndexes, "idx_sales_customer", new Document("customerId", 1));
            assertIndex(saleIndexes, "idx_sales_lines_crystal", new Document("lines.crystalId", 1));
        }
    }

    private void assertUniqueIndex(List<Document> indexes, String name, Document key) {
        Document index = findIndex(indexes, name);
        assertEquals(key, index.get("key"));
        assertEquals(true, index.getBoolean("unique"));
    }

    private void assertIndex(List<Document> indexes, String name, Document key) {
        assertEquals(key, findIndex(indexes, name).get("key"));
    }

    private Document findIndex(List<Document> indexes, String name) {
        return indexes.stream()
                .filter(index -> name.equals(index.getString("name")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing index " + name + " in " + indexes));
    }
}
