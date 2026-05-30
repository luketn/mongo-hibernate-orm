package com.luketn.crystalshop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luketn.crystalshop.domain.database.Crystal;
import com.luketn.crystalshop.domain.database.Customer;
import com.luketn.crystalshop.domain.database.InventoryItem;
import com.luketn.crystalshop.domain.database.Sale;
import com.luketn.crystalshop.domain.database.SaleLine;
import com.luketn.crystalshop.domain.database.Store;
import com.luketn.crystalshop.http.JsonSupport;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

final class SampleDataImporter {
    private final SessionFactory sessionFactory;
    private final ObjectMapper mapper = JsonSupport.createMapper();

    SampleDataImporter(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    Map<String, Object> importSampleData() {
        SampleData data = readSampleData();
        return inTransaction(session -> importData(session, data));
    }

    private Map<String, Object> importData(Session session, SampleData data) {
        clearDatabase(session);

        Map<String, Crystal> crystals = new HashMap<>();
        for (CrystalSeed seed : data.crystals()) {
            Crystal crystal = new Crystal(
                    seed.sku(),
                    seed.name(),
                    seed.family(),
                    seed.color(),
                    seed.origin(),
                    seed.retailPrice(),
                    seed.wholesaleCost()
            );
            session.persist(crystal);
            crystals.put(crystal.getSku(), crystal);
        }
        session.flush();

        Map<String, Customer> customers = new HashMap<>();
        for (CustomerSeed seed : data.customers()) {
            Customer customer = new Customer(seed.name(), seed.email(), seed.loyaltyTier());
            session.persist(customer);
            customers.put(customer.getEmail(), customer);
        }
        session.flush();

        int inventoryCount = 0;
        int saleCount = 0;
        int saleLineCount = 0;
        for (StoreSeed seed : data.stores()) {
            Store store = new Store(seed.code(), seed.name(), seed.city(), seed.address());
            session.persist(store);
            session.flush();

            for (InventorySeed inventorySeed : seed.inventory()) {
                Crystal crystal = requireSeedCrystal(crystals, inventorySeed.crystalSku());
                session.persist(new InventoryItem(store, crystal, inventorySeed.quantity(), inventorySeed.shelfLocation()));
                inventoryCount++;
            }

            for (SaleSeed saleSeed : seed.sales()) {
                Customer customer = requireSeedCustomer(customers, saleSeed.customerEmail());
                Sale sale = new Sale(store, customer, saleSeed.soldAt());
                for (SaleLineSeed lineSeed : saleSeed.lines()) {
                    Crystal crystal = requireSeedCrystal(crystals, lineSeed.crystalSku());
                    sale.addLine(new SaleLine(crystal, lineSeed.quantity(), lineSeed.unitPrice()));
                    saleLineCount++;
                }
                session.persist(sale);
                saleCount++;
            }
        }

        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("crystals", data.crystals().size());
        counts.put("customers", data.customers().size());
        counts.put("stores", data.stores().size());
        counts.put("inventoryItems", inventoryCount);
        counts.put("sales", saleCount);
        counts.put("saleLines", saleLineCount);
        return counts;
    }

    private SampleData readSampleData() {
        try (InputStream stream = SampleDataImporter.class.getClassLoader().getResourceAsStream("sample-data.json")) {
            if (stream == null) {
                throw new IllegalStateException("sample-data.json is missing from test resources");
            }
            return mapper.readValue(stream, SampleData.class);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read sample-data.json", e);
        }
    }

    private void clearDatabase(Session session) {
        session.createMutationQuery("delete from Sale").executeUpdate();
        session.createMutationQuery("delete from InventoryItem").executeUpdate();
        session.createMutationQuery("delete from Store").executeUpdate();
        session.createMutationQuery("delete from Customer").executeUpdate();
        session.createMutationQuery("delete from Crystal").executeUpdate();
    }

    private Crystal requireSeedCrystal(Map<String, Crystal> crystals, String sku) {
        Crystal crystal = crystals.get(sku);
        if (crystal == null) {
            throw new IllegalStateException("sample data references unknown crystal SKU " + sku);
        }
        return crystal;
    }

    private Customer requireSeedCustomer(Map<String, Customer> customers, String email) {
        Customer customer = customers.get(email);
        if (customer == null) {
            throw new IllegalStateException("sample data references unknown customer email " + email);
        }
        return customer;
    }

    private <T> T inTransaction(Function<Session, T> work) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            try {
                T result = work.apply(session);
                transaction.commit();
                return result;
            } catch (RuntimeException e) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw e;
            }
        }
    }

    private record SampleData(
            List<CrystalSeed> crystals,
            List<CustomerSeed> customers,
            List<StoreSeed> stores
    ) {
    }

    private record CrystalSeed(
            String sku,
            String name,
            String family,
            String color,
            String origin,
            BigDecimal retailPrice,
            BigDecimal wholesaleCost
    ) {
    }

    private record CustomerSeed(
            String name,
            String email,
            String loyaltyTier
    ) {
    }

    private record StoreSeed(
            String code,
            String name,
            String city,
            String address,
            List<InventorySeed> inventory,
            List<SaleSeed> sales
    ) {
    }

    private record InventorySeed(
            String crystalSku,
            int quantity,
            String shelfLocation
    ) {
    }

    private record SaleSeed(
            String customerEmail,
            LocalDateTime soldAt,
            List<SaleLineSeed> lines
    ) {
    }

    private record SaleLineSeed(
            String crystalSku,
            int quantity,
            BigDecimal unitPrice
    ) {
    }
}
