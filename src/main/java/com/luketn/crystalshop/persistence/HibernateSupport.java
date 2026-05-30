package com.luketn.crystalshop.persistence;

import com.luketn.crystalshop.AppConfig;
import com.luketn.crystalshop.domain.database.Crystal;
import com.luketn.crystalshop.domain.database.Customer;
import com.luketn.crystalshop.domain.database.InventoryItem;
import com.luketn.crystalshop.domain.database.Sale;
import com.luketn.crystalshop.domain.database.SaleLine;
import com.luketn.crystalshop.domain.database.Store;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import java.util.HashMap;
import java.util.Map;

public final class HibernateSupport {
    private HibernateSupport() {
    }

    public static SessionFactory createSessionFactory(AppConfig config) {
        return createSessionFactory(
                config,
                Crystal.class,
                Customer.class,
                Store.class,
                InventoryItem.class,
                Sale.class,
                SaleLine.class
        );
    }

    public static SessionFactory createSessionFactory(AppConfig config, Class<?>... annotatedClasses) {
        Map<String, Object> settings = new HashMap<>();
        settings.put("hibernate.dialect", "com.mongodb.hibernate.dialect.MongoDialect");
        settings.put("hibernate.connection.provider_class", "com.mongodb.hibernate.jdbc.MongoConnectionProvider");
        settings.put("jakarta.persistence.jdbc.url", config.databaseUrl());
        settings.put("hibernate.hbm2ddl.auto", config.schemaAction());
        settings.put("hibernate.show_sql", "false");
        settings.put("hibernate.format_sql", "false");
        settings.put("hibernate.highlight_sql", "false");

        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .applySettings(settings)
                .build();
        try {
            MetadataSources sources = new MetadataSources(registry);
            for (Class<?> annotatedClass : annotatedClasses) {
                sources.addAnnotatedClass(annotatedClass);
            }
            return sources.buildMetadata().buildSessionFactory();
        } catch (RuntimeException e) {
            StandardServiceRegistryBuilder.destroy(registry);
            throw e;
        }
    }
}
