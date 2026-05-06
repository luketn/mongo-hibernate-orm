package com.luketn.crystalshop.persistence;

import com.luketn.crystalshop.AppConfig;
import com.luketn.crystalshop.domain.Crystal;
import com.luketn.crystalshop.domain.Customer;
import com.luketn.crystalshop.domain.InventoryItem;
import com.luketn.crystalshop.domain.Sale;
import com.luketn.crystalshop.domain.SaleLine;
import com.luketn.crystalshop.domain.Store;
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
        Map<String, Object> settings = new HashMap<>();
        settings.put("jakarta.persistence.jdbc.driver", "org.postgresql.Driver");
        settings.put("jakarta.persistence.jdbc.url", config.jdbcUrl());
        settings.put("jakarta.persistence.jdbc.user", config.username());
        settings.put("jakarta.persistence.jdbc.password", config.password());
        settings.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        settings.put("hibernate.hbm2ddl.auto", config.hbm2ddlAuto());
        settings.put("hibernate.show_sql", "false");
        settings.put("hibernate.format_sql", "false");
        settings.put("hibernate.highlight_sql", "false");

        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .applySettings(settings)
                .build();
        try {
            return new MetadataSources(registry)
                    .addAnnotatedClass(Crystal.class)
                    .addAnnotatedClass(Customer.class)
                    .addAnnotatedClass(Store.class)
                    .addAnnotatedClass(InventoryItem.class)
                    .addAnnotatedClass(Sale.class)
                    .addAnnotatedClass(SaleLine.class)
                    .buildMetadata()
                    .buildSessionFactory();
        } catch (RuntimeException e) {
            StandardServiceRegistryBuilder.destroy(registry);
            throw e;
        }
    }
}
