package com.luketn.crystalshop.service;

import com.luketn.crystalshop.domain.api.CrystalRequest;
import com.luketn.crystalshop.domain.api.CrystalView;
import com.luketn.crystalshop.domain.api.CustomerRequest;
import com.luketn.crystalshop.domain.api.CustomerView;
import com.luketn.crystalshop.domain.api.InventoryItemRequest;
import com.luketn.crystalshop.domain.api.InventoryItemView;
import com.luketn.crystalshop.domain.api.SaleLineRequest;
import com.luketn.crystalshop.domain.api.SaleRequest;
import com.luketn.crystalshop.domain.api.SaleView;
import com.luketn.crystalshop.domain.api.StoreRequest;
import com.luketn.crystalshop.domain.api.StoreView;
import com.luketn.crystalshop.domain.database.Crystal;
import com.luketn.crystalshop.domain.database.Customer;
import com.luketn.crystalshop.domain.database.InventoryItem;
import com.luketn.crystalshop.domain.database.Sale;
import com.luketn.crystalshop.domain.database.SaleLine;
import com.luketn.crystalshop.domain.database.Store;
import com.luketn.crystalshop.http.ApiException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class CrystalShopService {
    private final SessionFactory sessionFactory;

    public CrystalShopService(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public List<CrystalView> listCrystals() {
        return inTransaction(session -> session.createQuery("from Crystal c order by c.id", Crystal.class)
                .getResultList()
                .stream()
                .map(CrystalView::from)
                .toList());
    }

    public CrystalView getCrystal(long id) {
        return inTransaction(session -> CrystalView.from(require(session, Crystal.class, id)));
    }

    public CrystalView createCrystal(CrystalRequest request) {
        return inTransaction(session -> {
            Crystal crystal = new Crystal(
                    requiredText(request.sku(), "sku"),
                    requiredText(request.name(), "name"),
                    requiredText(request.family(), "family"),
                    requiredText(request.color(), "color"),
                    requiredText(request.origin(), "origin"),
                    requiredDecimal(request.retailPrice(), "retailPrice"),
                    requiredDecimal(request.wholesaleCost(), "wholesaleCost")
            );
            session.persist(crystal);
            session.flush();
            return CrystalView.from(crystal);
        });
    }

    public CrystalView updateCrystal(long id, CrystalRequest request) {
        return inTransaction(session -> {
            Crystal crystal = require(session, Crystal.class, id);
            if (request.sku() != null) {
                crystal.setSku(requiredText(request.sku(), "sku"));
            }
            if (request.name() != null) {
                crystal.setName(requiredText(request.name(), "name"));
            }
            if (request.family() != null) {
                crystal.setFamily(requiredText(request.family(), "family"));
            }
            if (request.color() != null) {
                crystal.setColor(requiredText(request.color(), "color"));
            }
            if (request.origin() != null) {
                crystal.setOrigin(requiredText(request.origin(), "origin"));
            }
            if (request.retailPrice() != null) {
                crystal.setRetailPrice(requiredDecimal(request.retailPrice(), "retailPrice"));
            }
            if (request.wholesaleCost() != null) {
                crystal.setWholesaleCost(requiredDecimal(request.wholesaleCost(), "wholesaleCost"));
            }
            session.flush();
            return CrystalView.from(crystal);
        });
    }

    public void deleteCrystal(long id) {
        inTransaction(session -> {
            Crystal crystal = require(session, Crystal.class, id);
            long saleLineCount = session.createQuery(
                            "select count(line) from SaleLine line where line.crystal.id = :crystalId",
                            Long.class
                    )
                    .setParameter("crystalId", id)
                    .getSingleResult();
            long inventoryCount = session.createQuery(
                            "select count(item) from InventoryItem item where item.crystal.id = :crystalId",
                            Long.class
                    )
                    .setParameter("crystalId", id)
                    .getSingleResult();
            if (saleLineCount > 0 || inventoryCount > 0) {
                List<String> blockers = new ArrayList<>();
                if (saleLineCount > 0) {
                    blockers.add(saleLineCount + " sale line(s)");
                }
                if (inventoryCount > 0) {
                    blockers.add(inventoryCount + " inventory item(s)");
                }
                throw new ApiException(
                        409,
                        "Crystal " + crystal.getSku() + " cannot be deleted because it appears in "
                                + String.join(" and ", blockers)
                );
            }
            session.remove(crystal);
            return null;
        });
    }

    public List<CustomerView> listCustomers() {
        return inTransaction(session -> session.createQuery("from Customer c order by c.id", Customer.class)
                .getResultList()
                .stream()
                .map(CustomerView::from)
                .toList());
    }

    public CustomerView getCustomer(long id) {
        return inTransaction(session -> CustomerView.from(require(session, Customer.class, id)));
    }

    public CustomerView createCustomer(CustomerRequest request) {
        return inTransaction(session -> {
            Customer customer = new Customer(
                    requiredText(request.name(), "name"),
                    requiredText(request.email(), "email"),
                    requiredText(request.loyaltyTier(), "loyaltyTier")
            );
            session.persist(customer);
            session.flush();
            return CustomerView.from(customer);
        });
    }

    public CustomerView updateCustomer(long id, CustomerRequest request) {
        return inTransaction(session -> {
            Customer customer = require(session, Customer.class, id);
            if (request.name() != null) {
                customer.setName(requiredText(request.name(), "name"));
            }
            if (request.email() != null) {
                customer.setEmail(requiredText(request.email(), "email"));
            }
            if (request.loyaltyTier() != null) {
                customer.setLoyaltyTier(requiredText(request.loyaltyTier(), "loyaltyTier"));
            }
            session.flush();
            return CustomerView.from(customer);
        });
    }

    public void deleteCustomer(long id) {
        inTransaction(session -> {
            session.remove(require(session, Customer.class, id));
            return null;
        });
    }

    public List<StoreView> listStores() {
        return inTransaction(session -> session.createQuery("from Store s order by s.id", Store.class)
                .getResultList()
                .stream()
                .map(StoreView::from)
                .toList());
    }

    public StoreView getStore(long id) {
        return inTransaction(session -> StoreView.from(require(session, Store.class, id)));
    }

    public StoreView createStore(StoreRequest request) {
        return inTransaction(session -> {
            Store store = new Store(
                    requiredText(request.code(), "code"),
                    requiredText(request.name(), "name"),
                    requiredText(request.city(), "city"),
                    requiredText(request.address(), "address")
            );
            session.persist(store);
            session.flush();
            return StoreView.from(store);
        });
    }

    public StoreView updateStore(long id, StoreRequest request) {
        return inTransaction(session -> {
            Store store = require(session, Store.class, id);
            if (request.code() != null) {
                store.setCode(requiredText(request.code(), "code"));
            }
            if (request.name() != null) {
                store.setName(requiredText(request.name(), "name"));
            }
            if (request.city() != null) {
                store.setCity(requiredText(request.city(), "city"));
            }
            if (request.address() != null) {
                store.setAddress(requiredText(request.address(), "address"));
            }
            session.flush();
            return StoreView.from(store);
        });
    }

    public void deleteStore(long id) {
        inTransaction(session -> {
            session.remove(require(session, Store.class, id));
            return null;
        });
    }

    public List<InventoryItemView> listInventory() {
        return inTransaction(session -> session.createQuery("from InventoryItem i order by i.id", InventoryItem.class)
                .getResultList()
                .stream()
                .map(InventoryItemView::from)
                .toList());
    }

    public InventoryItemView getInventory(long id) {
        return inTransaction(session -> InventoryItemView.from(require(session, InventoryItem.class, id)));
    }

    public InventoryItemView createInventory(InventoryItemRequest request) {
        return inTransaction(session -> {
            Store store = require(session, Store.class, requiredLong(request.storeId(), "storeId"));
            Crystal crystal = require(session, Crystal.class, requiredLong(request.crystalId(), "crystalId"));
            InventoryItem item = new InventoryItem(
                    store,
                    crystal,
                    requiredInt(request.quantity(), "quantity", 0),
                    requiredText(request.shelfLocation(), "shelfLocation")
            );
            session.persist(item);
            session.flush();
            return InventoryItemView.from(item);
        });
    }

    public InventoryItemView updateInventory(long id, InventoryItemRequest request) {
        return inTransaction(session -> {
            InventoryItem item = require(session, InventoryItem.class, id);
            if (request.storeId() != null) {
                item.setStore(require(session, Store.class, requiredLong(request.storeId(), "storeId")));
            }
            if (request.crystalId() != null) {
                item.setCrystal(require(session, Crystal.class, requiredLong(request.crystalId(), "crystalId")));
            }
            if (request.quantity() != null) {
                item.setQuantity(requiredInt(request.quantity(), "quantity", 0));
            }
            if (request.shelfLocation() != null) {
                item.setShelfLocation(requiredText(request.shelfLocation(), "shelfLocation"));
            }
            session.flush();
            return InventoryItemView.from(item);
        });
    }

    public void deleteInventory(long id) {
        inTransaction(session -> {
            session.remove(require(session, InventoryItem.class, id));
            return null;
        });
    }

    public List<SaleView> listSales() {
        return inTransaction(session -> session.createQuery("from Sale s order by s.id", Sale.class)
                .getResultList()
                .stream()
                .map(SaleView::from)
                .toList());
    }

    public SaleView getSale(long id) {
        return inTransaction(session -> SaleView.from(require(session, Sale.class, id)));
    }

    public SaleView createSale(SaleRequest request) {
        return inTransaction(session -> {
            Store store = require(session, Store.class, requiredLong(request.storeId(), "storeId"));
            Customer customer = require(session, Customer.class, requiredLong(request.customerId(), "customerId"));
            Sale sale = new Sale(store, customer, requiredDateTime(request.soldAt(), "soldAt"));
            replaceSaleLines(session, sale, request.lines());
            session.persist(sale);
            session.flush();
            return SaleView.from(sale);
        });
    }

    public SaleView updateSale(long id, SaleRequest request) {
        return inTransaction(session -> {
            Sale sale = require(session, Sale.class, id);
            if (request.storeId() != null) {
                sale.setStore(require(session, Store.class, requiredLong(request.storeId(), "storeId")));
            }
            if (request.customerId() != null) {
                sale.setCustomer(require(session, Customer.class, requiredLong(request.customerId(), "customerId")));
            }
            if (request.soldAt() != null) {
                sale.setSoldAt(requiredDateTime(request.soldAt(), "soldAt"));
            }
            if (request.lines() != null) {
                replaceSaleLines(session, sale, request.lines());
            }
            session.flush();
            return SaleView.from(sale);
        });
    }

    public void deleteSale(long id) {
        inTransaction(session -> {
            session.remove(require(session, Sale.class, id));
            return null;
        });
    }

    private void replaceSaleLines(Session session, Sale sale, List<SaleLineRequest> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new ApiException(400, "lines must be a non-empty array");
        }
        sale.clearLines();
        for (SaleLineRequest line : lines) {
            if (line == null) {
                throw new ApiException(400, "lines entries must be objects");
            }
            Crystal crystal = require(session, Crystal.class, requiredLong(line.crystalId(), "crystalId"));
            sale.addLine(new SaleLine(
                    crystal,
                    requiredInt(line.quantity(), "quantity", 1),
                    requiredDecimal(line.unitPrice(), "unitPrice")
            ));
        }
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

    private String requiredText(String value, String field) {
        if (value == null) {
            throw new ApiException(400, field + " is required");
        }
        if (value.isBlank()) {
            throw new ApiException(400, field + " must be a non-empty string");
        }
        return value;
    }

    private BigDecimal requiredDecimal(BigDecimal value, String field) {
        if (value == null) {
            throw new ApiException(400, field + " is required");
        }
        return value;
    }

    private int requiredInt(Integer value, String field, int minimum) {
        if (value == null) {
            throw new ApiException(400, field + " is required");
        }
        if (value < minimum) {
            throw new ApiException(400, field + " must be at least " + minimum);
        }
        return value;
    }

    private long requiredLong(Long value, String field) {
        if (value == null) {
            throw new ApiException(400, field + " is required");
        }
        return value;
    }

    private LocalDateTime requiredDateTime(String value, String field) {
        if (value == null) {
            throw new ApiException(400, field + " is required");
        }
        if (value.isBlank()) {
            throw new ApiException(400, field + " must be an ISO-8601 local date-time string");
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            throw new ApiException(400, field + " must be an ISO-8601 local date-time string");
        }
    }
}
