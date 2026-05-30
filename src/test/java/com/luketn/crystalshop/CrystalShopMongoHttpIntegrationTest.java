package com.luketn.crystalshop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luketn.crystalshop.http.JsonSupport;
import com.luketn.crystalshop.persistence.HibernateSupport;
import org.bson.types.ObjectId;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class CrystalShopMongoHttpIntegrationTest {
    @Container
    static final MongoDBContainer mongo = new MongoDBContainer(MongoTestSupport.MONGO_IMAGE);

    static final ObjectMapper mapper = JsonSupport.createMapper();
    static HttpClient client;
    static CrystalShopApplication app;
    static Map<String, Object> seedCounts;

    @BeforeAll
    static void startApplication() {
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        try (SessionFactory sessionFactory = HibernateSupport.createSessionFactory(
                MongoTestSupport.mongoConfig(mongo, "none", 0)
        )) {
            seedCounts = new SampleDataImporter(sessionFactory).importSampleData();
        }
        app = CrystalShopApplication.start(MongoTestSupport.mongoConfig(mongo, "none", 0));
    }

    @AfterAll
    static void stopApplication() {
        if (app != null) {
            app.close();
        }
    }

    @Test
    void allEndpointsRoundTripThroughHttpHibernateAndMongo() throws Exception {
        assertTrue(requestText("GET", "/", null, 200).body().contains("<title>Crystal Shop</title>"));
        assertTrue(requestText("GET", "/styles.css", null, 200).body().contains(".workspace"));
        assertTrue(requestText("GET", "/app.js", null, 200).body().contains("const resources"));

        assertEquals(8, seedCounts.get("crystals"));
        assertEquals(18, seedCounts.get("inventoryItems"));
        assertEquals(39, seedCounts.get("sales"));

        JsonNode crystals = request("GET", "/crystals", null, 200).body();
        JsonNode customers = request("GET", "/customers", null, 200).body();
        JsonNode stores = request("GET", "/stores", null, 200).body();
        JsonNode inventory = request("GET", "/inventory", null, 200).body();
        JsonNode sales = request("GET", "/sales", null, 200).body();
        assertEquals(8, crystals.size());
        assertEquals(10, customers.size());
        assertEquals(3, stores.size());
        assertEquals(18, inventory.size());
        assertEquals(39, sales.size());
        assertTrue(sales.toString().contains("SEL-003 x2"));

        String seededSaleCrystalId = textId(findBy(crystals, "sku", "AME-001"));
        String inventoryCrystalId = textId(findBy(crystals, "sku", "LAB-004"));
        String seededCustomerId = textId(findBy(customers, "email", "mira.chen@example.com"));
        String seededStoreId = textId(findBy(stores, "code", "SYD-DAWN"));

        cannotDeleteCrystalWithPriorSales(seededSaleCrystalId);
        crystalCrud();
        customerCrud();
        storeCrud();
        inventoryCrud(seededStoreId, inventoryCrystalId);
        saleCrud(seededStoreId, seededCustomerId, seededSaleCrystalId);
    }

    private void crystalCrud() throws Exception {
        HttpResult created = request("POST", "/crystals", """
                {
                  "sku": "ROQ-999",
                  "name": "Rose Quartz Tumble",
                  "family": "Quartz",
                  "color": "Pink",
                  "origin": "South Africa",
                  "retailPrice": 12.25,
                  "wholesaleCost": 5.10
                }
                """, 201);
        String id = textId(created.body());
        assertEquals("ROQ-999", request("GET", "/crystals/" + id, null, 200).body().get("sku").asText());

        JsonNode updated = request("PUT", "/crystals/" + id, """
                {
                  "color": "Soft pink",
                  "retailPrice": 13.50
                }
                """, 200).body();
        assertEquals("Soft pink", updated.get("color").asText());
        assertEquals(0, new BigDecimal("13.50").compareTo(updated.get("retailPrice").decimalValue()));

        request("DELETE", "/crystals/" + id, null, 204);
        request("GET", "/crystals/" + id, null, 404);
    }

    private void cannotDeleteCrystalWithPriorSales(String crystalId) throws Exception {
        HttpResult delete = request("DELETE", "/crystals/" + crystalId, null, 409);
        assertTrue(delete.body().get("error").asText().contains("AME-001"));
        assertTrue(delete.body().get("error").asText().contains("sale line"));
        assertTrue(delete.body().get("error").asText().contains("inventory item"));

        JsonNode crystal = request("GET", "/crystals/" + crystalId, null, 200).body();
        assertEquals("AME-001", crystal.get("sku").asText());
    }

    private void customerCrud() throws Exception {
        HttpResult created = request("POST", "/customers", """
                {
                  "name": "Nora Wells",
                  "email": "nora.wells@example.com",
                  "loyaltyTier": "BRONZE"
                }
                """, 201);
        String id = textId(created.body());
        assertEquals("nora.wells@example.com", request("GET", "/customers/" + id, null, 200).body().get("email").asText());

        JsonNode updated = request("PUT", "/customers/" + id, """
                {
                  "loyaltyTier": "SILVER"
                }
                """, 200).body();
        assertEquals("SILVER", updated.get("loyaltyTier").asText());

        request("DELETE", "/customers/" + id, null, 204);
        request("GET", "/customers/" + id, null, 404);
    }

    private void storeCrud() throws Exception {
        HttpResult created = request("POST", "/stores", """
                {
                  "code": "BNE-SUN",
                  "name": "Sunstone Supply",
                  "city": "Brisbane",
                  "address": "5 River Terrace, Brisbane QLD"
                }
                """, 201);
        String id = textId(created.body());
        assertEquals("Sunstone Supply", request("GET", "/stores/" + id, null, 200).body().get("name").asText());

        JsonNode updated = request("PUT", "/stores/" + id, """
                {
                  "city": "Fortitude Valley"
                }
                """, 200).body();
        assertEquals("Fortitude Valley", updated.get("city").asText());

        request("DELETE", "/stores/" + id, null, 204);
        request("GET", "/stores/" + id, null, 404);
    }

    private void inventoryCrud(String storeId, String crystalId) throws Exception {
        HttpResult created = request("POST", "/inventory", """
                {
                  "storeId": "%s",
                  "crystalId": "%s",
                  "quantity": 3,
                  "shelfLocation": "Z9"
                }
                """.formatted(storeId, crystalId), 201);
        String id = textId(created.body());
        assertEquals("Z9", request("GET", "/inventory/" + id, null, 200).body().get("shelfLocation").asText());

        JsonNode updated = request("PUT", "/inventory/" + id, """
                {
                  "quantity": 4,
                  "shelfLocation": "Z10"
                }
                """, 200).body();
        assertEquals(4, updated.get("quantity").asInt());
        assertEquals("Z10", updated.get("shelfLocation").asText());

        request("DELETE", "/inventory/" + id, null, 204);
        request("GET", "/inventory/" + id, null, 404);
    }

    private void saleCrud(String storeId, String customerId, String crystalId) throws Exception {
        HttpResult created = request("POST", "/sales", """
                {
                  "storeId": "%s",
                  "customerId": "%s",
                  "soldAt": "2026-04-23T09:00:00",
                  "lines": [
                    {
                      "crystalId": "%s",
                      "quantity": 1,
                      "unitPrice": 48.00
                    }
                  ]
                }
                """.formatted(storeId, customerId, crystalId), 201);
        String id = textId(created.body());
        assertEquals(1, request("GET", "/sales/" + id, null, 200).body().get("lines").size());

        JsonNode updated = request("PUT", "/sales/" + id, """
                {
                  "soldAt": "2026-04-23T10:30:00",
                  "lines": [
                    {
                      "crystalId": "%s",
                      "quantity": 2,
                      "unitPrice": 47.50
                    }
                  ]
                }
                """.formatted(crystalId), 200).body();
        assertEquals("2026-04-23T10:30:00Z", updated.get("soldAt").asText());
        assertEquals(2, updated.get("lines").get(0).get("quantity").asInt());

        request("DELETE", "/sales/" + id, null, 204);
        request("GET", "/sales/" + id, null, 404);
    }

    private String textId(JsonNode node) {
        String id = node.get("id").asText();
        assertTrue(ObjectId.isValid(id), "Expected ObjectId hex string but got " + id);
        return id;
    }

    private JsonNode findBy(JsonNode array, String field, String value) {
        assertTrue(array.isArray(), "Expected an array");
        for (JsonNode node : array) {
            if (value.equals(node.get(field).asText())) {
                return node;
            }
        }
        throw new AssertionError("Could not find " + field + "=" + value + " in " + array);
    }

    private HttpResult request(String method, String path, String body, int expectedStatus)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .timeout(Duration.ofSeconds(10));
        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header("Content-Type", "application/json");
            builder.method(method, HttpRequest.BodyPublishers.ofString(body));
        }

        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(expectedStatus, response.statusCode(), response.body());
        if (response.body() == null || response.body().isBlank()) {
            return new HttpResult(response.statusCode(), mapper.createObjectNode());
        }
        JsonNode json = mapper.readTree(response.body());
        assertFalse(json.isMissingNode());
        return new HttpResult(response.statusCode(), json);
    }

    private TextResult requestText(String method, String path, String body, int expectedStatus)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .timeout(Duration.ofSeconds(10));
        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header("Content-Type", "text/plain");
            builder.method(method, HttpRequest.BodyPublishers.ofString(body));
        }

        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(expectedStatus, response.statusCode(), response.body());
        return new TextResult(response.statusCode(), response.body());
    }

    private URI uri(String path) {
        return app.baseUri().resolve(path);
    }

    private record HttpResult(int status, JsonNode body) {
    }

    private record TextResult(int status, String body) {
    }
}
