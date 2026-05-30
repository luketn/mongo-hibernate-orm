package com.luketn.crystalshop.domain.database;

import com.mongodb.hibernate.annotations.ObjectIdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales")
public class Sale {
    @Id
    @ObjectIdGenerator
    private ObjectId id;

    @Column(nullable = false)
    private ObjectId storeId;

    @Column(nullable = false)
    private ObjectId customerId;

    @Transient
    private Store store;

    @Transient
    private Customer customer;

    @Column(nullable = false)
    private Instant soldAt;

    private List<SaleLine> lines = new ArrayList<>();

    protected Sale() {
    }

    public Sale(Store store, Customer customer, LocalDateTime soldAt) {
        this(store, customer, toInstant(soldAt));
    }

    public Sale(Store store, Customer customer, Instant soldAt) {
        this(store.getId(), customer.getId(), soldAt);
        this.store = store;
        this.customer = customer;
    }

    public Sale(ObjectId storeId, ObjectId customerId, Instant soldAt) {
        this.storeId = storeId;
        this.customerId = customerId;
        this.soldAt = soldAt;
    }

    public ObjectId getId() {
        return id;
    }

    public ObjectId getStoreId() {
        return storeId;
    }

    public void setStoreId(ObjectId storeId) {
        this.storeId = storeId;
        this.store = null;
    }

    public ObjectId getCustomerId() {
        return customerId;
    }

    public void setCustomerId(ObjectId customerId) {
        this.customerId = customerId;
        this.customer = null;
    }

    public Store getStore() {
        return store;
    }

    public void setStore(Store store) {
        this.storeId = store.getId();
        this.store = store;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customerId = customer.getId();
        this.customer = customer;
    }

    public Instant getSoldAt() {
        return soldAt;
    }

    public void setSoldAt(Instant soldAt) {
        this.soldAt = soldAt;
    }

    public void setSoldAt(LocalDateTime soldAt) {
        this.soldAt = toInstant(soldAt);
    }

    public List<SaleLine> getLines() {
        return lines;
    }

    public void addLine(SaleLine line) {
        lines.add(line);
    }

    public void clearLines() {
        lines.clear();
    }

    private static Instant toInstant(LocalDateTime soldAt) {
        return soldAt.atOffset(ZoneOffset.UTC).toInstant();
    }
}
