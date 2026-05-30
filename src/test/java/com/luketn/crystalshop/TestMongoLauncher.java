package com.luketn.crystalshop;

import com.luketn.crystalshop.persistence.HibernateSupport;
import org.hibernate.SessionFactory;
import org.testcontainers.containers.MongoDBContainer;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

public final class TestMongoLauncher {
    private static final int MONGO_PORT = 27017;

    private TestMongoLauncher() {
    }

    public static void main(String[] args) throws InterruptedException {
        FixedPortMongoDBContainer mongo = new FixedPortMongoDBContainer();

        Runtime.getRuntime().addShutdownHook(new Thread(mongo::stop));
        mongo.start();

        String databaseUrl = mongo.getReplicaSetUrl();
        try (SessionFactory sessionFactory = HibernateSupport.createSessionFactory(new AppConfig(
                databaseUrl,
                "none",
                0
        ))) {
            Map<String, Object> counts = new SampleDataImporter(sessionFactory).importSampleData();
            System.out.println("Imported sample data: " + counts);
        }

        System.out.println("MongoDB is listening at " + databaseUrl);
        System.out.println("Start the real app with DB_URL='" + databaseUrl + "'. Press Ctrl+C to stop this container.");
        new CountDownLatch(1).await();
    }

    private static final class FixedPortMongoDBContainer extends MongoDBContainer {
        private FixedPortMongoDBContainer() {
            super(MongoTestSupport.MONGO_IMAGE);
            addFixedExposedPort(MONGO_PORT, MONGO_PORT);
        }
    }
}
