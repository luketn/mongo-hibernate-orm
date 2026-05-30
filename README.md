# Crystal Shop Hibernate ORM

A small Java 25 Maven application that exposes a Crystal Shop domain through the native JDK `HttpServer`, persists with the MongoDB Hibernate ORM extension, and verifies the full path with MongoDB Testcontainers.

## Run Tests

```sh
JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn test
```

## Run Service

Set a MongoDB replica-set connection and start the application:

```sh
JAVA_HOME=$(/usr/libexec/java_home -v 25) \
DB_URL='mongodb://localhost:27017/crystal_shop?replicaSet=rs0' \
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

## Test MongoDB Fixture

This starts a MongoDB Testcontainers replica set on the default MongoDB port, imports `src/test/resources/sample-data.json`, and keeps the container running so the real app can connect to the printed replica-set URI.

```sh
JAVA_HOME=$(/usr/libexec/java_home -v 25) \
mvn -q test-compile exec:java \
  -Dexec.classpathScope=test \
  -Dexec.mainClass=com.luketn.crystalshop.TestMongoLauncher
```

In another terminal:

```sh
JAVA_HOME=$(/usr/libexec/java_home -v 25) \
DB_URL='mongodb://localhost:27017/test' \
mvn -q exec:java -Dexec.mainClass=com.luketn.crystalshop.Main
```

If the fixture prints a different URI, use that value for `DB_URL`.

Port `27017` must be free before starting the fixture.
