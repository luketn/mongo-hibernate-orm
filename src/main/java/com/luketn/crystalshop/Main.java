package com.luketn.crystalshop;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws Exception {
        CrystalShopApplication app = CrystalShopApplication.start(AppConfig.fromEnvironment());
        Runtime.getRuntime().addShutdownHook(new Thread(app::close));
        System.out.printf("Crystal Shop service listening at %s%n", app.baseUri());
        Thread.currentThread().join();
    }
}
