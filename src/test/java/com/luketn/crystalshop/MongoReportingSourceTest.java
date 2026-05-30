package com.luketn.crystalshop;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

class MongoReportingSourceTest {
    @Test
    void reportingNoLongerContainsPostgreSqlNativeSql() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/luketn/crystalshop/service/CrystalShopReportingService.java"));

        assertFalse(source.contains("date_trunc("));
        assertFalse(source.contains("generate_series"));
        assertFalse(source.contains("join sale_lines"));
        assertFalse(source.contains("cast(:yearStart"));
        assertFalse(source.contains("::date"));
    }
}
