# Step 11 Notes: Mongo Reporting

- Replaced the PostgreSQL native-SQL reporting implementation with a Mongo driver-backed service.
- The reporting service now flattens yearly sale-line documents through a Mongo aggregation pipeline and computes the existing report DTOs from Mongo documents.
- Wired application startup to pass the Mongo database URL into the reporting service.
- Updated the reporting test to construct the Mongo reporting service from the Testcontainers replica-set URI.
- Re-enabled the Mongo HTTP report endpoint assertions now that reporting is Mongo-backed.
- Verified with `mvn-lt -Dtest=MongoReportingServiceTest,MongoReportingSourceTest,CrystalShopMongoHttpIntegrationTest test`.
