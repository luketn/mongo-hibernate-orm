package com.luketn.crystalshop;

import com.mongodb.hibernate.annotations.ObjectIdGenerator;
import com.luketn.crystalshop.persistence.HibernateSupport;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.bson.types.ObjectId;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Testcontainers
class MongoSessionFactoryBootTest {
    @Container
    static final MongoDBContainer mongo = new MongoDBContainer(MongoTestSupport.MONGO_IMAGE);

    @Test
    void bootsHibernateSessionFactoryAgainstMongoReplicaSet() {
        assertDoesNotThrow(() -> {
            try (SessionFactory sessionFactory = HibernateSupport.createSessionFactory(
                    MongoTestSupport.mongoConfig(mongo, "none", 0),
                    BootProbe.class
            )) {
                try (var session = sessionFactory.openSession()) {
                    var transaction = session.beginTransaction();
                    BootProbe probe = new BootProbe("ready");
                    session.persist(probe);
                    session.flush();
                    session.find(BootProbe.class, probe.getId());
                    transaction.commit();
                }
            }
        });
    }

    @Entity
    @Table(name = "boot_probe")
    public static class BootProbe {
        @Id
        @ObjectIdGenerator
        private ObjectId id;

        private String name;

        protected BootProbe() {
        }

        BootProbe(String name) {
            this.name = name;
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
    }
}
