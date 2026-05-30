# Step 04 Notes: MongoDB Boot Configuration

- Replaced PostgreSQL/JDBC settings in `HibernateSupport` with the MongoDB Hibernate extension settings:
  `MongoDialect`, `MongoConnectionProvider`, and the Mongo replica-set URI.
- Reworked `AppConfig` around `databaseUrl`, `schemaAction`, and `port`.
- Kept a temporary legacy constructor so the pre-migration PostgreSQL tests and helper code continue compiling until they are removed.
- Added an overload for building a `SessionFactory` with explicit annotated classes so the boot test can verify Mongo configuration independently from the domain mapping migration.
