package com.luketn.crystalshop.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luketn.crystalshop.service.CrystalShopService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class CrystalShopServer implements AutoCloseable {
    private final HttpServer server;
    private final ExecutorService executor;

    private CrystalShopServer(HttpServer server, ExecutorService executor) {
        this.server = server;
        this.executor = executor;
    }

    public static CrystalShopServer start(int port, CrystalShopService service) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            server.setExecutor(executor);
            server.createContext("/", new Router(service));
            server.start();
            return new CrystalShopServer(server, executor);
        } catch (IOException e) {
            throw new IllegalStateException("Could not start HTTP server", e);
        }
    }

    public URI baseUri() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }

    private static final class Router implements com.sun.net.httpserver.HttpHandler {
        private final CrystalShopService service;
        private final ObjectMapper mapper = JsonSupport.createMapper();

        private Router(CrystalShopService service) {
            this.service = service;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                route(exchange);
            } catch (ApiException e) {
                writeJson(exchange, e.statusCode(), error(e.statusCode(), e.getMessage()));
            } catch (JsonProcessingException e) {
                writeJson(exchange, 400, error(400, "Request body must be valid JSON"));
            } catch (Exception e) {
                writeJson(exchange, 500, error(500, e.getMessage()));
            } finally {
                exchange.close();
            }
        }

        private void route(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            List<String> segments = pathSegments(exchange.getRequestURI().getPath());

            if (segments.isEmpty()) {
                if ("GET".equals(method)) {
                    writeJson(exchange, 200, endpoints());
                    return;
                }
                throw methodNotAllowed();
            }

            if (segments.size() == 1 && "sample-data".equals(segments.getFirst())) {
                if ("POST".equals(method)) {
                    writeJson(exchange, 200, service.loadSampleData());
                    return;
                }
                throw methodNotAllowed();
            }

            if (segments.size() > 2) {
                throw new ApiException(404, "No endpoint found for " + exchange.getRequestURI().getPath());
            }

            String resource = segments.getFirst();
            JsonNode body = needsBody(method) ? readBody(exchange) : mapper.createObjectNode();
            if (segments.size() == 1) {
                handleCollection(exchange, method, resource, body);
            } else {
                handleItem(exchange, method, resource, parseId(segments.get(1)), body);
            }
        }

        private void handleCollection(HttpExchange exchange, String method, String resource, JsonNode body) throws IOException {
            switch (resource) {
                case "crystals" -> {
                    if ("GET".equals(method)) {
                        writeJson(exchange, 200, service.listCrystals());
                    } else if ("POST".equals(method)) {
                        writeJson(exchange, 201, service.createCrystal(body));
                    } else {
                        throw methodNotAllowed();
                    }
                }
                case "customers" -> {
                    if ("GET".equals(method)) {
                        writeJson(exchange, 200, service.listCustomers());
                    } else if ("POST".equals(method)) {
                        writeJson(exchange, 201, service.createCustomer(body));
                    } else {
                        throw methodNotAllowed();
                    }
                }
                case "stores" -> {
                    if ("GET".equals(method)) {
                        writeJson(exchange, 200, service.listStores());
                    } else if ("POST".equals(method)) {
                        writeJson(exchange, 201, service.createStore(body));
                    } else {
                        throw methodNotAllowed();
                    }
                }
                case "inventory" -> {
                    if ("GET".equals(method)) {
                        writeJson(exchange, 200, service.listInventory());
                    } else if ("POST".equals(method)) {
                        writeJson(exchange, 201, service.createInventory(body));
                    } else {
                        throw methodNotAllowed();
                    }
                }
                case "sales" -> {
                    if ("GET".equals(method)) {
                        writeJson(exchange, 200, service.listSales());
                    } else if ("POST".equals(method)) {
                        writeJson(exchange, 201, service.createSale(body));
                    } else {
                        throw methodNotAllowed();
                    }
                }
                default -> throw new ApiException(404, "No resource found for " + resource);
            }
        }

        private void handleItem(HttpExchange exchange, String method, String resource, long id, JsonNode body) throws IOException {
            switch (resource) {
                case "crystals" -> {
                    if ("GET".equals(method)) {
                        writeJson(exchange, 200, service.getCrystal(id));
                    } else if ("PUT".equals(method)) {
                        writeJson(exchange, 200, service.updateCrystal(id, body));
                    } else if ("DELETE".equals(method)) {
                        service.deleteCrystal(id);
                        writeNoContent(exchange);
                    } else {
                        throw methodNotAllowed();
                    }
                }
                case "customers" -> {
                    if ("GET".equals(method)) {
                        writeJson(exchange, 200, service.getCustomer(id));
                    } else if ("PUT".equals(method)) {
                        writeJson(exchange, 200, service.updateCustomer(id, body));
                    } else if ("DELETE".equals(method)) {
                        service.deleteCustomer(id);
                        writeNoContent(exchange);
                    } else {
                        throw methodNotAllowed();
                    }
                }
                case "stores" -> {
                    if ("GET".equals(method)) {
                        writeJson(exchange, 200, service.getStore(id));
                    } else if ("PUT".equals(method)) {
                        writeJson(exchange, 200, service.updateStore(id, body));
                    } else if ("DELETE".equals(method)) {
                        service.deleteStore(id);
                        writeNoContent(exchange);
                    } else {
                        throw methodNotAllowed();
                    }
                }
                case "inventory" -> {
                    if ("GET".equals(method)) {
                        writeJson(exchange, 200, service.getInventory(id));
                    } else if ("PUT".equals(method)) {
                        writeJson(exchange, 200, service.updateInventory(id, body));
                    } else if ("DELETE".equals(method)) {
                        service.deleteInventory(id);
                        writeNoContent(exchange);
                    } else {
                        throw methodNotAllowed();
                    }
                }
                case "sales" -> {
                    if ("GET".equals(method)) {
                        writeJson(exchange, 200, service.getSale(id));
                    } else if ("PUT".equals(method)) {
                        writeJson(exchange, 200, service.updateSale(id, body));
                    } else if ("DELETE".equals(method)) {
                        service.deleteSale(id);
                        writeNoContent(exchange);
                    } else {
                        throw methodNotAllowed();
                    }
                }
                default -> throw new ApiException(404, "No resource found for " + resource);
            }
        }

        private JsonNode readBody(HttpExchange exchange) throws IOException {
            byte[] bytes = exchange.getRequestBody().readAllBytes();
            if (bytes.length == 0) {
                return mapper.createObjectNode();
            }
            return mapper.readTree(bytes);
        }

        private void writeJson(HttpExchange exchange, int status, Object payload) throws IOException {
            byte[] bytes = mapper.writeValueAsBytes(payload);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream stream = exchange.getResponseBody()) {
                stream.write(bytes);
            }
        }

        private void writeNoContent(HttpExchange exchange) throws IOException {
            exchange.sendResponseHeaders(204, -1);
        }

        private List<String> pathSegments(String path) {
            return Arrays.stream(path.split("/"))
                    .filter(segment -> !segment.isBlank())
                    .toList();
        }

        private long parseId(String segment) {
            try {
                return Long.parseLong(segment);
            } catch (NumberFormatException e) {
                throw new ApiException(400, "id must be a long integer");
            }
        }

        private boolean needsBody(String method) {
            return "POST".equals(method) || "PUT".equals(method);
        }

        private ApiException methodNotAllowed() {
            return new ApiException(405, "Method is not allowed for this endpoint");
        }

        private Map<String, Object> endpoints() {
            Map<String, Object> routes = new LinkedHashMap<>();
            routes.put("sampleData", "POST /sample-data");
            routes.put("crystals", "GET|POST /crystals, GET|PUT|DELETE /crystals/{id}");
            routes.put("customers", "GET|POST /customers, GET|PUT|DELETE /customers/{id}");
            routes.put("stores", "GET|POST /stores, GET|PUT|DELETE /stores/{id}");
            routes.put("inventory", "GET|POST /inventory, GET|PUT|DELETE /inventory/{id}");
            routes.put("sales", "GET|POST /sales, GET|PUT|DELETE /sales/{id}");
            return routes;
        }

        private Map<String, Object> error(int status, String message) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", status);
            error.put("error", message == null ? "Unexpected server error" : message);
            return error;
        }
    }
}
