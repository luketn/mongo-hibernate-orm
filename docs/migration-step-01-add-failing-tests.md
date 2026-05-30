# Step 01 Notes: Add Failing MongoDB Tests

- Added the MongoDB Hibernate extension and MongoDB Testcontainers dependency only so Mongo-focused test sources can compile.
- Left the explicit Hibernate ORM 7.3.3.Final pin in place so dependency convergence remains red until the dependency alignment step.
- Added red tests for dependency convergence, Mongo SessionFactory boot, ObjectId/Instant/embedded-line mappings, HTTP string ID round trips, sample data import, reporting, browser ID handling, and Playwright.
- The expected initial failures are the current PostgreSQL connection settings, Hibernate 7.x runtime, Long ID API shape, relational entity associations, UI numeric ID parsing, and PostgreSQL-native reporting SQL.
