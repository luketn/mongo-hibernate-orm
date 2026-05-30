package com.luketn.crystalshop;

import com.luketn.crystalshop.domain.database.Crystal;
import com.luketn.crystalshop.persistence.HibernateSupport;
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
                    MongoTestSupport.mongoConfig(mongo, "none", 0)
            )) {
                try (var session = sessionFactory.openSession()) {
                    var transaction = session.beginTransaction();
                    session.createQuery("from Crystal c order by c.id", Crystal.class)
                            .setMaxResults(1)
                            .getResultList();
                    transaction.commit();
                }
            }
        });
    }
}
