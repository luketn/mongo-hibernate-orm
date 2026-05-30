package com.luketn.crystalshop.domain.database;

import com.mongodb.hibernate.annotations.ObjectIdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.bson.types.ObjectId;

@Entity
@Table(name = "inventory")
public class InventoryItem {
    @Id
    @ObjectIdGenerator
    private ObjectId id;

    @Column(nullable = false)
    private ObjectId storeId;

    @Column(nullable = false)
    private ObjectId crystalId;

    @Transient
    private Store store;

    @Transient
    private Crystal crystal;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "shelf_location", nullable = false)
    private String shelfLocation;

    protected InventoryItem() {
    }

    public InventoryItem(Store store, Crystal crystal, int quantity, String shelfLocation) {
        this(store.getId(), crystal.getId(), quantity, shelfLocation);
        this.store = store;
        this.crystal = crystal;
    }

    public InventoryItem(ObjectId storeId, ObjectId crystalId, int quantity, String shelfLocation) {
        this.storeId = storeId;
        this.crystalId = crystalId;
        this.quantity = quantity;
        this.shelfLocation = shelfLocation;
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

    public ObjectId getCrystalId() {
        return crystalId;
    }

    public void setCrystalId(ObjectId crystalId) {
        this.crystalId = crystalId;
        this.crystal = null;
    }

    public Store getStore() {
        return store;
    }

    public void setStore(Store store) {
        this.storeId = store.getId();
        this.store = store;
    }

    public Crystal getCrystal() {
        return crystal;
    }

    public void setCrystal(Crystal crystal) {
        this.crystalId = crystal.getId();
        this.crystal = crystal;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getShelfLocation() {
        return shelfLocation;
    }

    public void setShelfLocation(String shelfLocation) {
        this.shelfLocation = shelfLocation;
    }
}
