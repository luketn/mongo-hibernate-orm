package com.luketn.crystalshop;

import com.luketn.crystalshop.persistence.HibernateSupport;
import com.luketn.crystalshop.service.CrystalShopService;
import org.hibernate.SessionFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

public final class TestPostgresLauncher {
    private static final int POSTGRES_PORT = 5432;

    private TestPostgresLauncher() {
    }

    public static void main(String[] args) throws InterruptedException {
        FixedPortPostgreSQLContainer postgres = new FixedPortPostgreSQLContainer()
                .withDatabaseName("crystal_shop")
                .withUsername("postgres")
                .withPassword("postgres");

        Runtime.getRuntime().addShutdownHook(new Thread(postgres::stop));
        postgres.start();

        try (SessionFactory sessionFactory = HibernateSupport.createSessionFactory(new AppConfig(
                "jdbc:postgresql://127.0.0.1:5432/crystal_shop",
                "postgres",
                "postgres",
                "create",
                0
        ))) {
            CrystalShopService service = new CrystalShopService(sessionFactory);
            Map<String, Object> counts = service.loadSampleData();
            System.out.println("Imported sample data: " + counts);
        }

        System.out.println("PostgreSQL is listening at jdbc:postgresql://127.0.0.1:5432/crystal_shop");
        System.out.println("Credentials: postgres / postgres");
        System.out.println("Start the real app with its default DB settings. Press Ctrl+C to stop this container.");
        new CountDownLatch(1).await();
    }

    private static final class FixedPortPostgreSQLContainer extends PostgreSQLContainer<FixedPortPostgreSQLContainer> {
        private FixedPortPostgreSQLContainer() {
            super(DockerImageName.parse("postgres:16-alpine"));
            addFixedExposedPort(POSTGRES_PORT, POSTGRES_PORT);
        }
    }
}
