package com.luketn.crystalshop;

public record AppConfig(
        String jdbcUrl,
        String username,
        String password,
        String hbm2ddlAuto,
        int port
) {
    public static AppConfig fromEnvironment() {
        return new AppConfig(
                value("DB_URL", "jdbc:postgresql://localhost:5432/crystal_shop"),
                value("DB_USER", "postgres"),
                value("DB_PASSWORD", "postgres"),
                value("HIBERNATE_HBM2DDL_AUTO", "update"),
                Integer.parseInt(value("PORT", "8080"))
        );
    }

    private static String value(String name, String defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            value = System.getProperty(name);
        }
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
