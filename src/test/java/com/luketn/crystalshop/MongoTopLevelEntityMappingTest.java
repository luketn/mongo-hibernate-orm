package com.luketn.crystalshop;

import com.luketn.crystalshop.domain.database.Crystal;
import com.luketn.crystalshop.domain.database.Customer;
import com.luketn.crystalshop.domain.database.Store;
import com.luketn.crystalshop.persistence.HibernateSupport;
import org.bson.types.ObjectId;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers
class MongoTopLevelEntityMappingTest {
    @Container
    static final MongoDBContainer mongo = new MongoDBContainer(MongoTestSupport.MONGO_IMAGE);

    @Test
    void persistsCrystalsCustomersAndStoresWithObjectIdPrimaryKeys() {
        try (SessionFactory sessionFactory = HibernateSupport.createSessionFactory(
                MongoTestSupport.mongoConfig(mongo, "none", 0),
                Crystal.class,
                Customer.class,
                Store.class
        )) {
            try (var session = sessionFactory.openSession()) {
                var transaction = session.beginTransaction();

                Crystal crystal = new Crystal(
                        "TOP-001",
                        "Top Level Amethyst",
                        "Quartz",
                        "Violet",
                        "Uruguay",
                        new BigDecimal("40.00"),
                        new BigDecimal("15.00")
                );
                Customer customer = new Customer("Top Customer", "top.customer@example.com", "GOLD");
                Store store = new Store("TOP-STORE", "Top Store", "Sydney", "1 Test Street");
                session.persist(crystal);
                session.persist(customer);
                session.persist(store);
                session.flush();

                assertInstanceOf(ObjectId.class, crystal.getId());
                assertInstanceOf(ObjectId.class, customer.getId());
                assertInstanceOf(ObjectId.class, store.getId());
                assertNotNull(session.find(Crystal.class, crystal.getId()));
                assertEquals("top.customer@example.com", session.find(Customer.class, customer.getId()).getEmail());
                assertEquals("TOP-STORE", session.find(Store.class, store.getId()).getCode());

                transaction.commit();
            }
        }
    }
}
