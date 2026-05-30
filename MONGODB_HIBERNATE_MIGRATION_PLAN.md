# MongoDB Hibernate Migration Plan

## Migration Checklist

- [ ] Add the failing MongoDB tests first. Make only the minimal dependency additions needed for the test sources to compile. The first red tests should cover Mongo `SessionFactory` boot, dependency convergence, `ObjectId` IDs, `Instant` sale timestamps, embedded sale lines, sample data import, HTTP ID round trips, reporting, and Playwright.
- [ ] Run the new MongoDB tests and confirm they fail for expected migration reasons: current PostgreSQL config, `Long` IDs, relational associations, UI numeric ID parsing, and PostgreSQL SQL reporting.
- [ ] Align dependencies: add `org.mongodb:mongodb-hibernate:1.0.0-alpha0`, add `org.testcontainers:mongodb`, and remove or downgrade the explicit `hibernate-core` `7.3.3.Final` pin. The published extension POM currently depends on Hibernate ORM `6.6.34.Final`.
- [ ] Make the boot/config tests pass by replacing PostgreSQL connection settings with MongoDB settings, using `MongoDialect`, `MongoConnectionProvider`, and a replica-set MongoDB URI.
- [ ] Make the mapping tests pass by converting generated `Long` IDs to `ObjectId`, exposing IDs as opaque `String` values, converting `soldAt` to `Instant`, and embedding sale lines in `Sale`.
- [ ] Make the service/API/UI tests pass by updating lookups, delete checks, view mappers, request payloads, and browser ID handling so they use string/ObjectId IDs and do not rely on joins.
- [ ] Make the index test pass by creating Mongo indexes through the Java driver.
- [ ] Make the reporting test pass by replacing PostgreSQL SQL with `MongoSalesReportingService`, preferably using Java driver aggregation builders.
- [ ] Run the full MongoDB test suite, inspect generated documents/indexes, then remove PostgreSQL dependencies, defaults, and test helpers.

This document explains how to migrate the Crystal Shop application from PostgreSQL to MongoDB with the smallest practical code change set, using the official MongoDB Extension for Hibernate ORM.

The important conclusion is that the HTTP layer, API record names, browser screens, most CRUD endpoint shapes, and the general Hibernate `SessionFactory` transaction pattern can stay. The database connection settings, entity mapping, generated identifiers, relational associations, ID field types, UI ID parsing, and reporting native SQL cannot stay unchanged.

## Documentation Reviewed

Official MongoDB Hibernate ORM documentation reviewed:

- [Overview](https://www.mongodb.com/docs/languages/java/mongodb-hibernate/current/)
- [Get Started](https://www.mongodb.com/docs/languages/java/mongodb-hibernate/current/get-started/)
- [Model Your Data](https://www.mongodb.com/docs/languages/java/mongodb-hibernate/current/model-data/)
- [Entities](https://www.mongodb.com/docs/languages/java/mongodb-hibernate/current/model-data/entities/)
- [Interact with Data](https://www.mongodb.com/docs/languages/java/mongodb-hibernate/current/interact-data/)
- [CRUD Operations](https://www.mongodb.com/docs/languages/java/mongodb-hibernate/current/interact-data/crud/)
- [Specify a Query](https://www.mongodb.com/docs/languages/java/mongodb-hibernate/current/interact-data/specify-a-query/)
- [Native Queries](https://www.mongodb.com/docs/languages/java/mongodb-hibernate/current/interact-data/native-queries/)
- [Transactions and Sessions](https://www.mongodb.com/docs/languages/java/mongodb-hibernate/current/interact-data/transactions/)
- [Feature Compatibility](https://www.mongodb.com/docs/languages/java/mongodb-hibernate/current/feature-compatibility/)
- [Issues and Help](https://www.mongodb.com/docs/languages/java/mongodb-hibernate/current/issues-and-help/)
- [Version Compatibility](https://www.mongodb.com/docs/drivers/compatibility/?java-driver-framework=hibernate&language=java)
- [MongoDB Documents and `_id` behavior](https://www.mongodb.com/docs/current/core/document/)
- [Published Maven POM for `org.mongodb:mongodb-hibernate:1.0.0-alpha0`](https://repo1.maven.org/maven2/org/mongodb/mongodb-hibernate/1.0.0-alpha0/mongodb-hibernate-1.0.0-alpha0.pom)

Current status from the docs and the published Maven artifact, reviewed on 2026-05-30:

- The extension is public preview and intended for evaluation, not production deployment.
- The documented Maven dependency is `org.mongodb:mongodb-hibernate:1.0.0-alpha0`.
- The published `org.mongodb:mongodb-hibernate:1.0.0-alpha0` POM depends on `org.hibernate.orm:hibernate-core:6.6.34.Final` and `org.mongodb:mongodb-driver-sync:5.6.1`; this project currently pins Hibernate ORM `7.3.3.Final`, so dependency alignment is mandatory before implementation.
- Java 17 or later is required, so this Java 25 project is fine.
- Hibernate `Session`, `Transaction`, `persist`, `find`, `remove`, and simple HQL/JPQL queries are supported.
- Writes must run in transactions. The current service already wraps operations in a Hibernate transaction.
- The extension config uses `com.mongodb.hibernate.dialect.MongoDialect` and `com.mongodb.hibernate.jdbc.MongoConnectionProvider`.
- The extension supports MongoDB replica sets only. Standalone `mongod` instances are unsupported.
- Native queries use MongoDB Query Language / aggregation command syntax, not SQL.
- Native MQL parameter binding is not supported. Do not port the current `setParameter(...)` native-query pattern to Mongo reporting.
- Associations and lazy/eager fetching are unsupported in public preview. HQL joins are also unsupported. Native MongoDB aggregation can query multiple collections.
- Native projections are partially supported: when returning entity instances from `createNativeQuery()`, the `$project` stage must include each entity field except the primary key.
- Index creation is not supported through the extension. Use the MongoDB Java driver directly for indexes.
- Embedded documents are supported with Hibernate `@Embeddable` plus `@Struct`.
- The docs show Mongo `_id` mapping with `ObjectId` plus `@ObjectIdGenerator`.
- MongoDB itself does not require `_id` to be an `ObjectId`, but this migration should use BSON `ObjectId` values as the new document IDs.
- `java.time.Instant` is the documented temporal type for top-level and nested entities. The current `Sale.soldAt` uses `LocalDateTime`, so plan to switch persistence to `Instant` or prove `LocalDateTime` with a smoke test before keeping it.

## Current Application Shape

The code is already in a good position at the HTTP and API boundary:

- `src/main/java/com/luketn/crystalshop/http/CrystalShopServer.java` owns JSON serialization and routing.
- `src/main/java/com/luketn/crystalshop/domain/api` contains typed record DTOs.
- `src/main/java/com/luketn/crystalshop/service/CrystalShopService.java` exposes typed CRUD operations rather than `JsonNode`.
- `src/main/java/com/luketn/crystalshop/service/CrystalShopReportingService.java` keeps reporting separate from normal CRUD.
- `src/main/java/com/luketn/crystalshop/persistence/HibernateSupport.java` is already the central place that builds the Hibernate `SessionFactory`.

The database-specific parts are concentrated in:

- `pom.xml`, because it only has PostgreSQL and Testcontainers PostgreSQL dependencies today.
- `AppConfig`, because its fields are PostgreSQL/JDBC-shaped.
- `HibernateSupport`, because it hard-codes `org.postgresql.Driver` and `org.hibernate.dialect.PostgreSQLDialect`.
- `domain.database`, because the POJOs are relational entities with identity-generated `Long` IDs and association annotations.
- `CrystalShopService`, where relationship traversal, generated numeric IDs, and `LocalDateTime` persistence are assumed.
- `CrystalShopReportingService`, because it uses PostgreSQL SQL CTEs, `date_trunc`, `generate_series`, joins, and casts.
- Test helpers and tests, because they start PostgreSQL containers.
- `src/main/resources/web/app.js`, because select values and sale-line IDs are parsed with `Number.parseInt(...)`.

## Lowest-Code-Change Target

The recommended migration goal is:

- Keep all HTTP route names unchanged.
- Keep API record names and non-ID fields unchanged.
- Keep the browser screens and workflows unchanged, but update ID handling from numbers to strings.
- Keep `CrystalShopService` public method names and behavior, but change ID parameter types from `long` to `String`.
- Keep using Hibernate `SessionFactory`, `Session`, and transaction blocks.
- Replace PostgreSQL configuration and reporting SQL with MongoDB equivalents.
- Change entity mappings in the smallest possible way needed to make MongoDB work.

This is realistic for CRUD. It is not realistic for PostgreSQL reporting SQL. Reporting needs a MongoDB implementation behind the same output record types.

## Dependency Change

Add the MongoDB Hibernate extension:

```xml
<properties>
    <mongodb.hibernate.version>1.0.0-alpha0</mongodb.hibernate.version>
</properties>

<dependency>
    <groupId>org.mongodb</groupId>
    <artifactId>mongodb-hibernate</artifactId>
    <version>${mongodb.hibernate.version}</version>
</dependency>
```

For tests, add MongoDB Testcontainers while keeping PostgreSQL tests during the migration:

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mongodb</artifactId>
    <version>${testcontainers.version}</version>
    <scope>test</scope>
</dependency>
```

Do not leave the project pinned to Hibernate ORM `7.3.3.Final` while adding `mongodb-hibernate:1.0.0-alpha0`. The published artifact currently pulls `hibernate-core:6.6.34.Final`, so either remove this project's explicit `hibernate-core` dependency/property and let the extension choose its tested Hibernate line, or set `<hibernate.version>6.6.34.Final</hibernate.version>` for the migration branch. After changing dependencies, run `mvn dependency:tree` and confirm only one Hibernate ORM major/minor line is present before touching entity mappings.

The extension also pulls the synchronous MongoDB Java driver transitively. Add a direct `mongodb-driver-sync` dependency only if code uses the driver explicitly for indexes or reporting aggregations; doing so is reasonable here because the migration plan includes `MongoIndexBootstrap` and likely a driver-backed reporting service.

## Configuration Change

Replace the PostgreSQL-only config with MongoDB config:

```java
public record AppConfig(
        String databaseUrl,
        String schemaAction,
        int port
) {
}
```

Suggested environment variables:

- `DB_URL=mongodb://localhost:27017/crystal_shop?replicaSet=rs0` or an Atlas connection string for MongoDB
- `HIBERNATE_SCHEMA_ACTION=none|validate`
- `PORT=8080`

For MongoDB tests, use `MongoDBContainer#getReplicaSetUrl()` rather than a plain localhost URI. The Hibernate extension requires a replica set, and Testcontainers' MongoDB container is the easiest local way to satisfy that requirement.

`HibernateSupport` should switch to MongoDB settings:

```java
settings.put("hibernate.dialect", "com.mongodb.hibernate.dialect.MongoDialect");
settings.put("hibernate.connection.provider_class", "com.mongodb.hibernate.jdbc.MongoConnectionProvider");
settings.put("jakarta.persistence.jdbc.url", config.databaseUrl());
```

Do not rely on `hibernate.hbm2ddl.auto` to create Mongo indexes or constraints; that work belongs in the Mongo driver bootstrap.

## Entity Mapping Strategy

The existing `domain.database` classes are normal Hibernate POJOs. That should continue. Do not convert database entities to Java records, because Hibernate needs mutable entities with a no-arg constructor and managed fields.

Use a one-way swap and change the existing classes in `domain.database` in place:

- Keep class names: `Crystal`, `Customer`, `Store`, `InventoryItem`, `Sale`, `SaleLine`.
- Keep API records in `domain.api`.
- Keep `CrystalShopService` method names and behavior, but change ID parameter types where needed.
- Replace relational annotations and fields with Mongo-compatible mappings.

Expected entity changes:

- Replace the existing `Long id` fields with BSON `ObjectId` IDs.
- Replace `@GeneratedValue(strategy = GenerationType.IDENTITY)` because it is a PostgreSQL identity-column strategy.
- Use MongoDB Hibernate's `@ObjectIdGenerator` mapping for new documents.
- Convert API record ID fields from `long` to `String`, using the canonical ObjectId hex string at the HTTP boundary.
- Treat old PostgreSQL numeric IDs as migration-only source IDs, not as application IDs after the cutover.
- Replace JPA relationships that rely on joins with either embedded documents or explicit ID fields. Remove `@ManyToOne`, `@OneToMany`, `@JoinColumn`, `mappedBy`, `orphanRemoval`, and lazy/eager fetch assumptions from Mongo-mapped entities.
- Prefer embedding sale lines inside `Sale`, because sale lines are naturally owned by a sale and are loaded together in the UI.
- Keep `Crystal`, `Customer`, and `Store` as top-level collections.
- Model `InventoryItem` as its own top-level collection with `storeId` and `crystalId` fields, unless it is always loaded by store.
- Change `Sale.soldAt` persistence from `LocalDateTime` to `Instant`, or add a focused smoke test proving `LocalDateTime` works with the exact extension/Hibernate versions before deciding to keep it.
- Revisit SQL-shaped `@Column(name = "...")` mappings such as `retail_price`, `wholesale_cost`, `sold_at`, `shelf_location`, and `unit_price`. If the desired MongoDB document fields are camelCase as shown in this plan, remove those snake_case overrides or explicitly update the data model proposal to match the stored BSON field names.

This keeps the application architecture stable, but it will touch private service logic where the current code passes entity references such as `new InventoryItem(store, crystal, ...)`.

## CRUD Service Impact

The current `CrystalShopService` already uses Hibernate APIs that the MongoDB extension supports:

- `session.persist(...)`
- `session.find(...)`
- `session.remove(...)`
- `session.flush()`
- `session.createQuery(...)` for simple list and count queries
- explicit Hibernate transactions

Likely unchanged:

- public method names and endpoint behavior, except ID argument types
- validation helper methods
- request and response record names and non-ID fields
- most simple list queries such as `from Crystal c order by c.id`
- transaction wrapper

Likely changed:

- Public ID parameters and API record ID fields, because HTTP IDs should become ObjectId hex strings instead of numeric `long` values.
- `require(session, type, id)`, because it should parse a string to `ObjectId` before calling `session.find(...)`.
- Inventory and sale writes, because current constructors take entity references.
- Delete-blocker queries, because current HQL navigates relationship fields like `line.crystal.id`; Mongo entities should query direct ID fields or use driver counts against embedded sale lines.
- `SaleView.from(...)` and `InventoryItemView.from(...)` if database entities no longer hold full related entity objects.
- `Sale.soldAt` conversion, because the API currently uses a local date-time string while Mongo persistence should use a tested `Instant` mapping.

For minimum migration risk, keep route shapes the same but allow the path segment to be a string ObjectId:

```text
GET /api/crystals/663b8c944f5f7d7a19d42a11
```

The browser UI should treat IDs as opaque strings. It should not parse, increment, or format them as numbers.

Concrete UI changes in `src/main/resources/web/app.js`:

- keep `row.id` and `state.selectedId` as strings;
- keep `<option>.value = row.id` as-is, but stop passing select values through `Number.parseInt(...)`;
- replace `numberValue(...)` with a string ID helper for `storeId`, `customerId`, `crystalId`, and sale-line `crystalId`;
- keep quantity/year/decimal fields numeric.

## Reporting Service Impact

`CrystalShopReportingService` is PostgreSQL-specific. It currently depends on:

- SQL CTEs
- `date_trunc`
- `generate_series`
- SQL joins
- PostgreSQL casts like `::date`
- SQL aggregate expressions

The MongoDB Hibernate docs are clear that `createNativeQuery()` takes MongoDB Query Language statements, including aggregation pipelines. PostgreSQL CTEs will not run against MongoDB.

There is also a second reporting-specific constraint: native MQL parameter binding is not supported. The current helper builds SQL once and calls `query.setParameter("yearStart", ...)` and `query.setParameter("nextYear", ...)`; that pattern cannot be reused for Mongo native queries. For fixed report inputs such as `year`, either build MQL from validated numeric/date constants or use the MongoDB Java driver's aggregation builders and typed `Document`/POJO mapping.

Replace the current reporting class with `MongoSalesReportingService`, containing MongoDB aggregation pipelines. The HTTP server and UI can keep returning the existing API records:

- `AnnualSalesReport`
- `WeeklySalesTrend`
- `MonthlyCustomerRetention`
- `ReportTotals`
- `ProductSalesInsight`
- `ProductForecast`

Prefer aggregation pipelines over trying to express the report in HQL. Public-preview HQL aggregate and join support is limited, while native Mongo aggregation is the documented escape hatch. For this report's custom DTO output, the Java driver is likely cleaner than `createNativeQuery()` because native Hibernate projections are constrained around returning entity-shaped documents.

## MongoDB Data Model Proposal

For a minimum migration that still fits MongoDB reasonably well:

```text
crystals
  _id ObjectId
  sku
  name
  family
  color
  origin
  retailPrice
  wholesaleCost

customers
  _id ObjectId
  name
  email
  loyaltyTier

stores
  _id ObjectId
  code
  name
  city
  address

inventory
  _id ObjectId
  storeId
  crystalId
  quantity
  shelfLocation

sales
  _id ObjectId
  storeId
  customerId
  soldAt Instant
  lines [
    {
      crystalId
      crystalSku
      crystalName
      wholesaleCostAtSale
      quantity
      unitPrice
    }
  ]
```

`storeId`, `customerId`, and `crystalId` are also `ObjectId` values. During migration, the importer should build per-table maps from old PostgreSQL numeric IDs to new MongoDB `ObjectId` values, then rewrite all references through those maps.

This design keeps the existing top-level concepts and endpoints. It embeds sale lines because they are dependent rows that are always meaningful inside a sale. It avoids relying on unsupported HQL joins for the core object graph.

If the reporting page needs product names and costs during aggregation, the MongoDB reporting service can either:

- use `$lookup` stages from `sales.lines.crystalId` to `crystals._id`, or
- denormalize immutable sale-line facts such as `crystalSku`, `crystalName`, and `wholesaleCostAtSale` into each sale line.

For analytics, denormalizing facts into sale lines is usually better. It makes historical reports stable even if a product name or cost changes later.

## Indexes and Constraints

PostgreSQL currently enforces foreign keys, identity generation, and unique constraints. MongoDB Hibernate public preview does not create indexes through Hibernate.

Add a small Mongo bootstrap helper using the MongoDB Java driver directly for:

- unique index on `crystals.sku`
- unique index on `customers.email`
- unique index on `stores.code`
- unique compound index on `inventory.storeId` plus `inventory.crystalId`
- lookup indexes on `inventory.storeId`, `inventory.crystalId`, `sales.storeId`, `sales.customerId`, and `sales.lines.crystalId`

This helper should be separate from Hibernate, for example:

```text
src/main/java/com/luketn/crystalshop/persistence/MongoIndexBootstrap.java
```

Run it during application startup after the MongoDB connection is available.

## Sample Data Migration

The current sample data importer is test-only and already flows through Hibernate. Keep that behavior.

For MongoDB:

- Keep `src/test/resources/sample-data.json` as the source fixture.
- Add `TestMongoLauncher`, analogous to `TestPostgresLauncher`.
- Start `MongoDBContainer`.
- Use the replica-set connection string returned by Testcontainers.
- Start the app with `DB_URL=<container connection string>`.
- Import sample data through the same importer or through HTTP endpoints.

The current test fixture does not contain numeric source IDs; it references crystals by SKU, customers by email, and stores by the store object currently being imported. That is good for MongoDB because the importer can allocate fresh `ObjectId` values and keep using those natural-key maps.

For a real PostgreSQL-to-MongoDB data export, old numeric IDs should remain migration-only source identifiers. The Mongo importer should:

- allocate a new `ObjectId` for every crystal, customer, store, inventory item, and sale;
- keep in-memory maps such as `oldCrystalId -> newCrystalObjectId`;
- rewrite inventory references, sale references, and sale-line references through those maps;
- optionally persist `legacyPostgresId` for audit/debug only, not for app lookup behavior.

The current `SampleDataImporter.clearDatabase(...)` uses HQL mutation queries. Verify that `delete from Entity` works against the Mongo extension in the importer smoke test; if not, clear collections with the MongoDB Java driver in test setup.

## Test Plan

Use a test-first sequence. Add the MongoDB tests before changing production code, run them, and keep the failures focused on the next migration step.

New tests:

- `MongoDependencyConvergenceTest`
- `MongoSessionFactoryBootTest`
- `MongoEntityMappingTest`
- `CrystalShopMongoHttpIntegrationTest`
- `MongoSampleDataImporterTest`
- `MongoReportingServiceTest`
- `CrystalShopMongoPlaywrightTest`

Initial failing-test sequence:

1. Add `MongoDependencyConvergenceTest` to prove the runtime classpath does not include Hibernate ORM `7.3.3.Final` after dependency alignment.
2. Add `MongoSessionFactoryBootTest` using `MongoDBContainer#getReplicaSetUrl()`. It should initially fail because `HibernateSupport` is still PostgreSQL-only.
3. Add `MongoEntityMappingTest` for a simple persist/find/remove, `ObjectId` IDs, `Instant soldAt`, and an embedded sale with multiple lines. It should initially fail because the entities are still relational.
4. Add `CrystalShopMongoHttpIntegrationTest` for CRUD, string IDs, delete blockers, and sale round trips. It should initially fail because services and routes still use `long` IDs and association traversal.
5. Add `MongoSampleDataImporterTest` to prove the fixture loads into MongoDB and preserves counts/reference display fields.
6. Add `MongoReportingServiceTest` for the 2025 annual report output. It should initially fail because reporting is still PostgreSQL SQL.
7. Add `CrystalShopMongoPlaywrightTest` after the HTTP flow is close, to prove the browser handles string IDs and report rendering.

Test flows:

- Start MongoDB with Testcontainers.
- Build a Mongo-backed `SessionFactory`.
- Confirm dependency convergence so Hibernate ORM `7.3.3.Final` is not still on the runtime classpath.
- Load sample data.
- Persist/find/remove a simple top-level entity with `ObjectId`.
- Persist/find a sale with embedded lines and a `soldAt` value using the chosen temporal type.
- Call every HTTP endpoint.
- Create, read, update, and delete crystals, customers, stores, inventory, and sales.
- Verify a sale with multiple lines survives round trip.
- Verify deleting a crystal with prior sales is rejected.
- Verify deleting a crystal with no sale lines or inventory succeeds.
- Drive the full browser UI with Playwright.
- Drive the annual sales report page for 2025 and verify non-empty charts and projections.

Keep the PostgreSQL tests running as a temporary baseline until MongoDB tests pass with equivalent assertions, then remove the PostgreSQL-specific tests and helpers.

## Implementation Order

1. Add the first failing MongoDB tests: dependency convergence, `SessionFactory` boot, entity mapping, HTTP CRUD, importer, reporting, and Playwright. Add only the dependency changes needed for these tests to compile.
2. Run the targeted MongoDB tests and confirm they fail for the expected reasons before production code changes.
3. Align dependencies and make the dependency convergence test pass.
4. Replace `AppConfig` and `HibernateSupport` with MongoDB connection settings and make the boot test pass.
5. Convert API IDs, route IDs, request relationship IDs, and browser ID handling to opaque `String` values.
6. Convert `Crystal`, `Customer`, and `Store` to Mongo entities.
7. Convert `InventoryItem` to explicit `ObjectId storeId` and `ObjectId crystalId` fields.
8. Convert `Sale` to embedded sale lines with `ObjectId` references, denormalized sale-line facts, and an `Instant` timestamp.
9. Update service lookups, delete checks, view mappers, and sample data import until the HTTP/importer tests pass.
10. Add Mongo indexes with the Java driver bootstrap helper and verify them in tests.
11. Replace reporting SQL with MongoDB aggregation pipelines and make the reporting test pass.
12. Run Playwright against the Mongo-backed app, then remove PostgreSQL dependencies, defaults, test containers, and helper code.

## Expected Code Change Size

Low-change areas:

- HTTP routes
- JSON serialization boundary
- API record names and non-ID fields
- UI screens and client-side route names
- Most request validation
- Test fixture domain concepts

Medium-change areas:

- `pom.xml`
- `AppConfig`
- `HibernateSupport`
- Testcontainers setup
- entity ID generation
- API/service ID types moving from `long` to `String`
- UI ID parsing and payload construction
- `Sale.soldAt` temporal mapping
- service methods that currently use entity associations

High-change areas:

- reporting rewrite from PostgreSQL SQL to MongoDB aggregation
- relational association mapping
- database-enforced constraints and foreign keys

## Key Risks

- The extension is public preview, so API and behavior can change.
- The current explicit Hibernate ORM `7.3.3.Final` pin conflicts with the extension artifact's Hibernate `6.6.34.Final` dependency unless it is aligned.
- The extension requires MongoDB replica sets; plain standalone local `mongod` is unsupported.
- HQL joins are not supported in public preview.
- HQL aggregate support is limited, which affects reporting.
- Native MQL parameter binding is not supported, so dynamic report inputs must be handled carefully.
- Native MQL projections are entity-shaped; custom report DTOs are probably cleaner through the Java driver.
- `LocalDateTime` is not the documented temporal type; `Instant` is.
- Native SQL cannot be reused; Mongo native queries are MongoDB command/aggregation syntax.
- Indexes must be created through the MongoDB Java driver, not Hibernate.
- PostgreSQL foreign-key behavior must be recreated in service logic, embedded documents, or Mongo validation/index strategy.
- ID migration requires a reliable old-ID-to-new-ObjectId mapping while importing related records.

## Final Target

The final application should be MongoDB-only while keeping the architecture intact:

- MongoDB config,
- same HTTP route names, API record names, and main service method names,
- Mongo-compatible database POJOs,
- Mongo-specific reporting service behind the existing report response records.
