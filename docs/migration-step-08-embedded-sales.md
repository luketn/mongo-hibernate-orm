# Step 08 Notes: Embedded Sales

- Converted `Sale` primary keys to generated `ObjectId` values.
- Replaced sale `Store` and `Customer` associations with explicit `ObjectId storeId` and `ObjectId customerId` references.
- Converted `Sale.soldAt` from `LocalDateTime` persistence to `Instant`, using UTC conversion for temporary compatibility constructors/setters.
- Converted `SaleLine` from a top-level entity to an embedded `@Struct` value with ObjectId crystal references and denormalized SKU/name/cost-at-sale facts.
- Removed `SaleLine` from the default annotated entity list.
- Verified with `mvn-lt -Dtest=MongoEntityMappingTest test`.
