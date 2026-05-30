package com.luketn.crystalshop;

import com.luketn.crystalshop.http.CrystalShopServer;
import com.luketn.crystalshop.persistence.HibernateSupport;
import com.luketn.crystalshop.persistence.MongoIndexBootstrap;
import com.luketn.crystalshop.service.CrystalShopService;
import com.luketn.crystalshop.service.CrystalShopReportingService;
import org.hibernate.SessionFactory;

import java.net.URI;

public final class CrystalShopApplication implements AutoCloseable {
    private final SessionFactory sessionFactory;
    private final CrystalShopServer server;

    private CrystalShopApplication(SessionFactory sessionFactory, CrystalShopServer server) {
        this.sessionFactory = sessionFactory;
        this.server = server;
    }

    public static CrystalShopApplication start(AppConfig config) {
        SessionFactory sessionFactory = HibernateSupport.createSessionFactory(config);
        MongoIndexBootstrap.createIndexes(config.databaseUrl());
        CrystalShopService service = new CrystalShopService(sessionFactory);
        CrystalShopReportingService reportingService = new CrystalShopReportingService(config.databaseUrl());
        CrystalShopServer server = CrystalShopServer.start(config.port(), service, reportingService);
        return new CrystalShopApplication(sessionFactory, server);
    }

    public URI baseUri() {
        return server.baseUri();
    }

    @Override
    public void close() {
        server.close();
        sessionFactory.close();
    }
}
