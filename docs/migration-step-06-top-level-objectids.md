# Step 06 Notes: Top-Level ObjectId Entities

- Converted `Crystal`, `Customer`, and `Store` primary keys from generated `Long` values to MongoDB `ObjectId` values.
- Added `@ObjectIdGenerator` to those top-level entities.
- Added a focused Mongo mapping test that persists and finds the three independent entities with real ObjectId primary keys.
- Verified with `mvn-lt -Dtest=MongoTopLevelEntityMappingTest test`.
