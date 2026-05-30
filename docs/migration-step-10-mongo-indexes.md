# Step 10 Notes: Mongo Indexes

- Added a direct `mongodb-driver-sync` dependency because application code now uses the Java driver directly.
- Added `MongoIndexBootstrap` to create unique natural-key indexes and lookup indexes that Hibernate MongoDB public preview does not create.
- Wired index creation into application startup.
- Added a Mongo integration test that inspects real index metadata for names, keys, and uniqueness.
- Verified with `mvn-lt -Dtest=MongoIndexBootstrapTest test`.
