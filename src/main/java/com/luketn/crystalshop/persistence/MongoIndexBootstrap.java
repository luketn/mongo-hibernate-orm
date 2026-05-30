package com.luketn.crystalshop.persistence;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

public final class MongoIndexBootstrap {
    private MongoIndexBootstrap() {
    }

    public static void createIndexes(String databaseUrl) {
        ConnectionString connectionString = new ConnectionString(databaseUrl);
        try (var client = MongoClients.create(connectionString)) {
            createIndexes(client.getDatabase(databaseName(connectionString)));
        }
    }

    public static void createIndexes(MongoDatabase database) {
        database.getCollection("crystals")
                .createIndex(Indexes.ascending("sku"), unique("uk_crystals_sku"));
        database.getCollection("customers")
                .createIndex(Indexes.ascending("email"), unique("uk_customers_email"));
        database.getCollection("stores")
                .createIndex(Indexes.ascending("code"), unique("uk_stores_code"));

        database.getCollection("inventory")
                .createIndex(Indexes.ascending("storeId", "crystalId"), unique("uk_inventory_store_crystal"));
        database.getCollection("inventory")
                .createIndex(Indexes.ascending("storeId"), named("idx_inventory_store"));
        database.getCollection("inventory")
                .createIndex(Indexes.ascending("crystalId"), named("idx_inventory_crystal"));

        database.getCollection("sales")
                .createIndex(Indexes.ascending("storeId"), named("idx_sales_store"));
        database.getCollection("sales")
                .createIndex(Indexes.ascending("customerId"), named("idx_sales_customer"));
        database.getCollection("sales")
                .createIndex(Indexes.ascending("lines.crystalId"), named("idx_sales_lines_crystal"));
    }

    private static IndexOptions unique(String name) {
        return named(name).unique(true);
    }

    private static IndexOptions named(String name) {
        return new IndexOptions().name(name);
    }

    private static String databaseName(ConnectionString connectionString) {
        String database = connectionString.getDatabase();
        return database == null || database.isBlank() ? "test" : database;
    }
}
