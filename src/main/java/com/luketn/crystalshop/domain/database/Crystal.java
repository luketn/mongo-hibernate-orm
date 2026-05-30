package com.luketn.crystalshop.domain.database;

import com.mongodb.hibernate.annotations.ObjectIdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.bson.types.ObjectId;

import java.math.BigDecimal;

@Entity
@Table(name = "crystals")
public class Crystal {
    @Id
    @ObjectIdGenerator
    private ObjectId id;

    @Column(nullable = false, unique = true, length = 32)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String family;

    @Column(nullable = false)
    private String color;

    @Column(nullable = false)
    private String origin;

    @Column(name = "retail_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal retailPrice;

    @Column(name = "wholesale_cost", precision = 10, scale = 2)
    private BigDecimal wholesaleCost;

    protected Crystal() {
    }

    public Crystal(
            String sku,
            String name,
            String family,
            String color,
            String origin,
            BigDecimal retailPrice,
            BigDecimal wholesaleCost
    ) {
        this.sku = sku;
        this.name = name;
        this.family = family;
        this.color = color;
        this.origin = origin;
        this.retailPrice = retailPrice;
        this.wholesaleCost = wholesaleCost;
    }

    public ObjectId getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFamily() {
        return family;
    }

    public void setFamily(String family) {
        this.family = family;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public BigDecimal getRetailPrice() {
        return retailPrice;
    }

    public void setRetailPrice(BigDecimal retailPrice) {
        this.retailPrice = retailPrice;
    }

    public BigDecimal getWholesaleCost() {
        return wholesaleCost;
    }

    public void setWholesaleCost(BigDecimal wholesaleCost) {
        this.wholesaleCost = wholesaleCost;
    }
}
