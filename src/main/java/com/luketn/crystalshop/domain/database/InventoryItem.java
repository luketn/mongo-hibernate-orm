package com.luketn.crystalshop.domain.database;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "inventory_items",
        uniqueConstraints = @UniqueConstraint(name = "uk_inventory_store_crystal", columnNames = {"store_id", "crystal_id"})
)
public class InventoryItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crystal_id", nullable = false)
    private Crystal crystal;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "shelf_location", nullable = false)
    private String shelfLocation;

    protected InventoryItem() {
    }

    public InventoryItem(Store store, Crystal crystal, int quantity, String shelfLocation) {
        this.store = store;
        this.crystal = crystal;
        this.quantity = quantity;
        this.shelfLocation = shelfLocation;
    }

    public Long getId() {
        return id;
    }

    public Store getStore() {
        return store;
    }

    public void setStore(Store store) {
        this.store = store;
    }

    public Crystal getCrystal() {
        return crystal;
    }

    public void setCrystal(Crystal crystal) {
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
