# Crystal Shop Hibernate ORM

A small Java 25 Maven application that exposes a Crystal Shop domain through the native JDK `HttpServer`, persists with classic Hibernate ORM, and verifies the full path with PostgreSQL Testcontainers.

## Run Tests

```sh
JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn test
```

## Run Service

Set a PostgreSQL connection and start the application:

```sh
JAVA_HOME=$(/usr/libexec/java_home -v 25) \
DB_URL=jdbc:postgresql://localhost:5432/crystal_shop \
DB_USER=postgres \
DB_PASSWORD=postgres \
mvn -q exec:java -Dexec.mainClass=com.luketn.crystalshop.Main
```

The service exposes:

- `POST /sample-data`
- `GET|POST /crystals`, `GET|PUT|DELETE /crystals/{id}`
- `GET|POST /customers`, `GET|PUT|DELETE /customers/{id}`
- `GET|POST /stores`, `GET|PUT|DELETE /stores/{id}`
- `GET|POST /inventory`, `GET|PUT|DELETE /inventory/{id}`
- `GET|POST /sales`, `GET|PUT|DELETE /sales/{id}`
