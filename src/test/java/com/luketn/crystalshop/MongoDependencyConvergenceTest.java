package com.luketn.crystalshop;

import org.hibernate.Version;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MongoDependencyConvergenceTest {
    @Test
    void mongodbHibernateExtensionControlsTheHibernateRuntimeLine() {
        assertDoesNotThrow(() -> Class.forName("com.mongodb.hibernate.dialect.MongoDialect"));
        assertDoesNotThrow(() -> Class.forName("com.mongodb.hibernate.jdbc.MongoConnectionProvider"));
        assertEquals("6.6.34.Final", Version.getVersionString());
    }
}
