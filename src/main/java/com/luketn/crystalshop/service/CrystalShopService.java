package com.luketn.crystalshop.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luketn.crystalshop.domain.Crystal;
import com.luketn.crystalshop.domain.Customer;
import com.luketn.crystalshop.domain.InventoryItem;
import com.luketn.crystalshop.domain.Sale;
import com.luketn.crystalshop.domain.SaleLine;
import com.luketn.crystalshop.domain.Store;
import com.luketn.crystalshop.http.ApiException;
import com.luketn.crystalshop.http.JsonSupport;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class CrystalShopService {
    private final SessionFactory sessionFactory;
    private final ObjectMapper mapper = JsonSupport.createMapper();

    public CrystalShopService(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public Map<String, Object> loadSampleData() {
        SampleData data = readSampleData();
        return inTransaction(session -> {
            clearDatabase(session);

            Map<String, Crystal> crystals = new HashMap<>();
            for (CrystalSeed seed : data.crystals()) {
                Crystal crystal = new Crystal(
                        seed.sku(),
                        seed.name(),
                        seed.family(),
                        seed.color(),
                        seed.origin(),
                        seed.retailPrice()
                );
                session.persist(crystal);
                crystals.put(crystal.getSku(), crystal);
            }

            Map<String, Customer> customers = new HashMap<>();
            for (CustomerSeed seed : data.customers()) {
                Customer customer = new Customer(seed.name(), seed.email(), seed.loyaltyTier());
                session.persist(customer);
                customers.put(customer.getEmail(), customer);
            }

            int inventoryCount = 0;
            int saleCount = 0;
            int saleLineCount = 0;
            for (StoreSeed seed : data.stores()) {
                Store store = new Store(seed.code(), seed.name(), seed.city(), seed.address());
                session.persist(store);

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
        });
    }

    public List<Map<String, Object>> listCrystals() {
        return inTransaction(session -> session.createQuery("from Crystal c order by c.id", Crystal.class)
                .getResultList()
                .stream()
                .map(this::crystalDto)
                .toList());
    }

    public Map<String, Object> getCrystal(long id) {
        return inTransaction(session -> crystalDto(require(session, Crystal.class, id)));
    }

    public Map<String, Object> createCrystal(JsonNode body) {
        return inTransaction(session -> {
            Crystal crystal = new Crystal(
                    requiredText(body, "sku"),
                    requiredText(body, "name"),
                    requiredText(body, "family"),
                    requiredText(body, "color"),
                    requiredText(body, "origin"),
                    requiredDecimal(body, "retailPrice")
            );
            session.persist(crystal);
            session.flush();
            return crystalDto(crystal);
        });
    }

    public Map<String, Object> updateCrystal(long id, JsonNode body) {
        return inTransaction(session -> {
            Crystal crystal = require(session, Crystal.class, id);
            optionalText(body, "sku", crystal::setSku);
            optionalText(body, "name", crystal::setName);
            optionalText(body, "family", crystal::setFamily);
            optionalText(body, "color", crystal::setColor);
            optionalText(body, "origin", crystal::setOrigin);
            optionalDecimal(body, "retailPrice", crystal::setRetailPrice);
            session.flush();
            return crystalDto(crystal);
        });
    }

    public void deleteCrystal(long id) {
        inTransaction(session -> {
            session.remove(require(session, Crystal.class, id));
            return null;
        });
    }

    public List<Map<String, Object>> listCustomers() {
        return inTransaction(session -> session.createQuery("from Customer c order by c.id", Customer.class)
                .getResultList()
                .stream()
                .map(this::customerDto)
                .toList());
    }

    public Map<String, Object> getCustomer(long id) {
        return inTransaction(session -> customerDto(require(session, Customer.class, id)));
    }

    public Map<String, Object> createCustomer(JsonNode body) {
        return inTransaction(session -> {
            Customer customer = new Customer(
                    requiredText(body, "name"),
                    requiredText(body, "email"),
                    requiredText(body, "loyaltyTier")
            );
            session.persist(customer);
            session.flush();
            return customerDto(customer);
        });
    }

    public Map<String, Object> updateCustomer(long id, JsonNode body) {
        return inTransaction(session -> {
            Customer customer = require(session, Customer.class, id);
            optionalText(body, "name", customer::setName);
            optionalText(body, "email", customer::setEmail);
            optionalText(body, "loyaltyTier", customer::setLoyaltyTier);
            session.flush();
            return customerDto(customer);
        });
    }

    public void deleteCustomer(long id) {
        inTransaction(session -> {
            session.remove(require(session, Customer.class, id));
            return null;
        });
    }

    public List<Map<String, Object>> listStores() {
        return inTransaction(session -> session.createQuery("from Store s order by s.id", Store.class)
                .getResultList()
                .stream()
                .map(this::storeDto)
                .toList());
    }

    public Map<String, Object> getStore(long id) {
        return inTransaction(session -> storeDto(require(session, Store.class, id)));
    }

    public Map<String, Object> createStore(JsonNode body) {
        return inTransaction(session -> {
            Store store = new Store(
                    requiredText(body, "code"),
                    requiredText(body, "name"),
                    requiredText(body, "city"),
                    requiredText(body, "address")
            );
            session.persist(store);
            session.flush();
            return storeDto(store);
        });
    }

    public Map<String, Object> updateStore(long id, JsonNode body) {
        return inTransaction(session -> {
            Store store = require(session, Store.class, id);
            optionalText(body, "code", store::setCode);
            optionalText(body, "name", store::setName);
            optionalText(body, "city", store::setCity);
            optionalText(body, "address", store::setAddress);
            session.flush();
            return storeDto(store);
        });
    }

    public void deleteStore(long id) {
        inTransaction(session -> {
            session.remove(require(session, Store.class, id));
            return null;
        });
    }

    public List<Map<String, Object>> listInventory() {
        return inTransaction(session -> session.createQuery("from InventoryItem i order by i.id", InventoryItem.class)
                .getResultList()
                .stream()
                .map(this::inventoryDto)
                .toList());
    }

    public Map<String, Object> getInventory(long id) {
        return inTransaction(session -> inventoryDto(require(session, InventoryItem.class, id)));
    }

    public Map<String, Object> createInventory(JsonNode body) {
        return inTransaction(session -> {
            Store store = require(session, Store.class, requiredLong(body, "storeId"));
            Crystal crystal = require(session, Crystal.class, requiredLong(body, "crystalId"));
            InventoryItem item = new InventoryItem(
                    store,
                    crystal,
                    requiredInt(body, "quantity", 0),
                    requiredText(body, "shelfLocation")
            );
            session.persist(item);
            session.flush();
            return inventoryDto(item);
        });
    }

    public Map<String, Object> updateInventory(long id, JsonNode body) {
        return inTransaction(session -> {
            InventoryItem item = require(session, InventoryItem.class, id);
            optionalLong(body, "storeId", storeId -> item.setStore(require(session, Store.class, storeId)));
            optionalLong(body, "crystalId", crystalId -> item.setCrystal(require(session, Crystal.class, crystalId)));
            optionalInt(body, "quantity", 0, item::setQuantity);
            optionalText(body, "shelfLocation", item::setShelfLocation);
            session.flush();
            return inventoryDto(item);
        });
    }

    public void deleteInventory(long id) {
        inTransaction(session -> {
            session.remove(require(session, InventoryItem.class, id));
            return null;
        });
    }

    public List<Map<String, Object>> listSales() {
        return inTransaction(session -> session.createQuery("from Sale s order by s.id", Sale.class)
                .getResultList()
                .stream()
                .map(this::saleDto)
                .toList());
    }

    public Map<String, Object> getSale(long id) {
        return inTransaction(session -> saleDto(require(session, Sale.class, id)));
    }

    public Map<String, Object> createSale(JsonNode body) {
        return inTransaction(session -> {
            Store store = require(session, Store.class, requiredLong(body, "storeId"));
            Customer customer = require(session, Customer.class, requiredLong(body, "customerId"));
            Sale sale = new Sale(store, customer, requiredDateTime(body, "soldAt"));
            replaceSaleLines(session, sale, requiredArray(body, "lines"));
            session.persist(sale);
            session.flush();
            return saleDto(sale);
        });
    }

    public Map<String, Object> updateSale(long id, JsonNode body) {
        return inTransaction(session -> {
            Sale sale = require(session, Sale.class, id);
            optionalLong(body, "storeId", storeId -> sale.setStore(require(session, Store.class, storeId)));
            optionalLong(body, "customerId", customerId -> sale.setCustomer(require(session, Customer.class, customerId)));
            optionalDateTime(body, "soldAt", sale::setSoldAt);
            if (body.has("lines")) {
                replaceSaleLines(session, sale, requiredArray(body, "lines"));
            }
            session.flush();
            return saleDto(sale);
        });
    }

    public void deleteSale(long id) {
        inTransaction(session -> {
            session.remove(require(session, Sale.class, id));
            return null;
        });
    }

    private void replaceSaleLines(Session session, Sale sale, JsonNode lines) {
        if (!lines.isArray() || lines.isEmpty()) {
            throw new ApiException(400, "lines must be a non-empty array");
        }
        sale.clearLines();
        for (JsonNode line : lines) {
            Crystal crystal = require(session, Crystal.class, requiredLong(line, "crystalId"));
            sale.addLine(new SaleLine(
                    crystal,
                    requiredInt(line, "quantity", 1),
                    requiredDecimal(line, "unitPrice")
            ));
        }
    }

    private SampleData readSampleData() {
        try (InputStream stream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream("sample-data.json")) {
            if (stream == null) {
                throw new ApiException(500, "sample-data.json is missing from resources");
            }
            return mapper.readValue(stream, SampleData.class);
        } catch (IOException e) {
            throw new ApiException(500, "Could not read sample-data.json: " + e.getMessage());
        }
    }

    private void clearDatabase(Session session) {
        session.createMutationQuery("delete from SaleLine").executeUpdate();
        session.createMutationQuery("delete from Sale").executeUpdate();
        session.createMutationQuery("delete from InventoryItem").executeUpdate();
        session.createMutationQuery("delete from Store").executeUpdate();
        session.createMutationQuery("delete from Customer").executeUpdate();
        session.createMutationQuery("delete from Crystal").executeUpdate();
    }

    private Crystal requireSeedCrystal(Map<String, Crystal> crystals, String sku) {
        Crystal crystal = crystals.get(sku);
        if (crystal == null) {
            throw new ApiException(400, "sample data references unknown crystal SKU " + sku);
        }
        return crystal;
    }

    private Customer requireSeedCustomer(Map<String, Customer> customers, String email) {
        Customer customer = customers.get(email);
        if (customer == null) {
            throw new ApiException(400, "sample data references unknown customer email " + email);
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

    private <T> T require(Session session, Class<T> type, long id) {
        T entity = session.find(type, id);
        if (entity == null) {
            throw new ApiException(404, type.getSimpleName() + " " + id + " was not found");
        }
        return entity;
    }

    private Map<String, Object> crystalDto(Crystal crystal) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", crystal.getId());
        dto.put("sku", crystal.getSku());
        dto.put("name", crystal.getName());
        dto.put("family", crystal.getFamily());
        dto.put("color", crystal.getColor());
        dto.put("origin", crystal.getOrigin());
        dto.put("retailPrice", crystal.getRetailPrice());
        return dto;
    }

    private Map<String, Object> customerDto(Customer customer) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", customer.getId());
        dto.put("name", customer.getName());
        dto.put("email", customer.getEmail());
        dto.put("loyaltyTier", customer.getLoyaltyTier());
        return dto;
    }

    private Map<String, Object> storeDto(Store store) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", store.getId());
        dto.put("code", store.getCode());
        dto.put("name", store.getName());
        dto.put("city", store.getCity());
        dto.put("address", store.getAddress());
        return dto;
    }

    private Map<String, Object> inventoryDto(InventoryItem item) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", item.getId());
        dto.put("storeId", item.getStore().getId());
        dto.put("storeCode", item.getStore().getCode());
        dto.put("storeName", item.getStore().getName());
        dto.put("crystalId", item.getCrystal().getId());
        dto.put("crystalSku", item.getCrystal().getSku());
        dto.put("crystalName", item.getCrystal().getName());
        dto.put("quantity", item.getQuantity());
        dto.put("shelfLocation", item.getShelfLocation());
        return dto;
    }

    private Map<String, Object> saleDto(Sale sale) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", sale.getId());
        dto.put("storeId", sale.getStore().getId());
        dto.put("storeCode", sale.getStore().getCode());
        dto.put("storeName", sale.getStore().getName());
        dto.put("customerId", sale.getCustomer().getId());
        dto.put("customerEmail", sale.getCustomer().getEmail());
        dto.put("customerName", sale.getCustomer().getName());
        dto.put("soldAt", sale.getSoldAt().toString());
        dto.put("lines", sale.getLines().stream().map(this::saleLineDto).toList());
        dto.put("total", sale.getLines().stream()
                .map(line -> line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return dto;
    }

    private Map<String, Object> saleLineDto(SaleLine line) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", line.getId());
        dto.put("crystalId", line.getCrystal().getId());
        dto.put("crystalSku", line.getCrystal().getSku());
        dto.put("crystalName", line.getCrystal().getName());
        dto.put("quantity", line.getQuantity());
        dto.put("unitPrice", line.getUnitPrice());
        dto.put("lineTotal", line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity())));
        return dto;
    }

    private String requiredText(JsonNode body, String field) {
        JsonNode value = required(body, field);
        if (!value.isTextual() || value.asText().isBlank()) {
            throw new ApiException(400, field + " must be a non-empty string");
        }
        return value.asText();
    }

    private BigDecimal requiredDecimal(JsonNode body, String field) {
        JsonNode value = required(body, field);
        try {
            if (value.isNumber()) {
                return value.decimalValue();
            }
            if (value.isTextual() && !value.asText().isBlank()) {
                return new BigDecimal(value.asText());
            }
        } catch (NumberFormatException e) {
            throw new ApiException(400, field + " must be a decimal number");
        }
        throw new ApiException(400, field + " must be a decimal number");
    }

    private int requiredInt(JsonNode body, String field, int minimum) {
        JsonNode value = required(body, field);
        if (!value.canConvertToInt()) {
            throw new ApiException(400, field + " must be an integer");
        }
        int parsed = value.asInt();
        if (parsed < minimum) {
            throw new ApiException(400, field + " must be at least " + minimum);
        }
        return parsed;
    }

    private long requiredLong(JsonNode body, String field) {
        JsonNode value = required(body, field);
        if (!value.canConvertToLong()) {
            throw new ApiException(400, field + " must be a long integer");
        }
        return value.asLong();
    }

    private LocalDateTime requiredDateTime(JsonNode body, String field) {
        JsonNode value = required(body, field);
        if (!value.isTextual() || value.asText().isBlank()) {
            throw new ApiException(400, field + " must be an ISO-8601 local date-time string");
        }
        try {
            return LocalDateTime.parse(value.asText());
        } catch (DateTimeParseException e) {
            throw new ApiException(400, field + " must be an ISO-8601 local date-time string");
        }
    }

    private JsonNode requiredArray(JsonNode body, String field) {
        JsonNode value = required(body, field);
        if (!value.isArray()) {
            throw new ApiException(400, field + " must be an array");
        }
        return value;
    }

    private JsonNode required(JsonNode body, String field) {
        JsonNode value = body.get(field);
        if (value == null || value.isNull()) {
            throw new ApiException(400, field + " is required");
        }
        return value;
    }

    private void optionalText(JsonNode body, String field, Consumer<String> setter) {
        if (body.has(field)) {
            setter.accept(requiredText(body, field));
        }
    }

    private void optionalDecimal(JsonNode body, String field, Consumer<BigDecimal> setter) {
        if (body.has(field)) {
            setter.accept(requiredDecimal(body, field));
        }
    }

    private void optionalInt(JsonNode body, String field, int minimum, Consumer<Integer> setter) {
        if (body.has(field)) {
            setter.accept(requiredInt(body, field, minimum));
        }
    }

    private void optionalLong(JsonNode body, String field, Consumer<Long> setter) {
        if (body.has(field)) {
            setter.accept(requiredLong(body, field));
        }
    }

    private void optionalDateTime(JsonNode body, String field, Consumer<LocalDateTime> setter) {
        if (body.has(field)) {
            setter.accept(requiredDateTime(body, field));
        }
    }
}
