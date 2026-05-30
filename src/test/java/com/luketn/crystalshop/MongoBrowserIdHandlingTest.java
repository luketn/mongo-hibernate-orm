package com.luketn.crystalshop;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MongoBrowserIdHandlingTest {
    @Test
    void browserTreatsRelationshipIdsAsOpaqueStrings() throws IOException {
        String app = Files.readString(Path.of("src/main/resources/web/app.js"));

        assertTrue(app.contains("stringIdValue(form, \"storeId\")"));
        assertTrue(app.contains("stringIdValue(form, \"customerId\")"));
        assertTrue(app.contains("crystalId: requiredStringId("));
        assertFalse(app.contains("storeId: numberValue(form, \"storeId\")"));
        assertFalse(app.contains("customerId: numberValue(form, \"customerId\")"));
    }
}
