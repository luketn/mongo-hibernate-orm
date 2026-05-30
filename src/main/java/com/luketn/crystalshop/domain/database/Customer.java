package com.luketn.crystalshop.domain.database;

import com.mongodb.hibernate.annotations.ObjectIdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.bson.types.ObjectId;

@Entity
@Table(name = "customers")
public class Customer {
    @Id
    @ObjectIdGenerator
    private ObjectId id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "loyalty_tier", nullable = false)
    private String loyaltyTier;

    protected Customer() {
    }

    public Customer(String name, String email, String loyaltyTier) {
        this.name = name;
        this.email = email;
        this.loyaltyTier = loyaltyTier;
    }

    public ObjectId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLoyaltyTier() {
        return loyaltyTier;
    }

    public void setLoyaltyTier(String loyaltyTier) {
        this.loyaltyTier = loyaltyTier;
    }
}
