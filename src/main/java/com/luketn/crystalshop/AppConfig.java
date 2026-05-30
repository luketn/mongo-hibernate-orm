package com.luketn.crystalshop;

public record AppConfig(
        String databaseUrl,
        String schemaAction,
        int port
) {
    public static AppConfig fromEnvironment() {
        return new AppConfig(
                value("DB_URL", "mongodb://localhost:27017/crystal_shop?replicaSet=rs0"),
                value("HIBERNATE_SCHEMA_ACTION", "none"),
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
