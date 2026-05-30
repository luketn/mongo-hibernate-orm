package com.luketn.crystalshop.domain.database;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;
import org.bson.types.ObjectId;
import org.hibernate.annotations.Struct;

import java.math.BigDecimal;

@Embeddable
@Struct(name = "SaleLine")
public class SaleLine {
    private ObjectId id = new ObjectId();

    private ObjectId crystalId;

    private String crystalSku;

    private String crystalName;

    private BigDecimal wholesaleCostAtSale;

    @Transient
    private Crystal crystal;

    private int quantity;

    private BigDecimal unitPrice;

    protected SaleLine() {
    }

    public SaleLine(Crystal crystal, int quantity, BigDecimal unitPrice) {
        this(
                crystal.getId(),
                crystal.getSku(),
                crystal.getName(),
                crystal.getWholesaleCost(),
                quantity,
                unitPrice
        );
        this.crystal = crystal;
    }

    public SaleLine(
            ObjectId crystalId,
            String crystalSku,
            String crystalName,
            BigDecimal wholesaleCostAtSale,
            int quantity,
            BigDecimal unitPrice
    ) {
        this.crystalId = crystalId;
        this.crystalSku = crystalSku;
        this.crystalName = crystalName;
        this.wholesaleCostAtSale = wholesaleCostAtSale;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public ObjectId getId() {
        return id;
    }

    public ObjectId getCrystalId() {
        return crystalId;
    }

    public void setCrystalId(ObjectId crystalId) {
        this.crystalId = crystalId;
        this.crystal = null;
    }

    public String getCrystalSku() {
        return crystalSku;
    }

    public void setCrystalSku(String crystalSku) {
        this.crystalSku = crystalSku;
    }

    public String getCrystalName() {
        return crystalName;
    }

    public void setCrystalName(String crystalName) {
        this.crystalName = crystalName;
    }

    public BigDecimal getWholesaleCostAtSale() {
        return wholesaleCostAtSale;
    }

    public void setWholesaleCostAtSale(BigDecimal wholesaleCostAtSale) {
        this.wholesaleCostAtSale = wholesaleCostAtSale;
    }

    public Crystal getCrystal() {
        return crystal;
    }

    public void setCrystal(Crystal crystal) {
        this.crystalId = crystal.getId();
        this.crystalSku = crystal.getSku();
        this.crystalName = crystal.getName();
        this.wholesaleCostAtSale = crystal.getWholesaleCost();
        this.crystal = crystal;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
}
