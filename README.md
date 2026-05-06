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

- `GET /` browser GUI
- `GET /api` endpoint list
- `GET|POST /crystals`, `GET|PUT|DELETE /crystals/{id}`
- `GET|POST /customers`, `GET|PUT|DELETE /customers/{id}`
- `GET|POST /stores`, `GET|PUT|DELETE /stores/{id}`
- `GET|POST /inventory`, `GET|PUT|DELETE /inventory/{id}`
- `GET|POST /sales`, `GET|PUT|DELETE /sales/{id}`

## Test PostgreSQL Fixture

This starts a PostgreSQL Testcontainers instance on the default PostgreSQL port, imports `src/test/resources/sample-data.json`, and keeps the container running so the real app can connect with its default settings.

```sh
JAVA_HOME=$(/usr/libexec/java_home -v 25) \
mvn -q test-compile exec:java \
  -Dexec.classpathScope=test \
  -Dexec.mainClass=com.luketn.crystalshop.TestPostgresLauncher
```

In another terminal:

```sh
JAVA_HOME=$(/usr/libexec/java_home -v 25) \
mvn -q exec:java -Dexec.mainClass=com.luketn.crystalshop.Main
```

Port `5432` must be free before starting the fixture.
