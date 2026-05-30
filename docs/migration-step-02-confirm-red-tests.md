# Step 02 Notes: Confirm MongoDB Tests Fail First

- Ran `mvn-lt -Dtest='Mongo*Test,CrystalShopMongoHttpIntegrationTest,CrystalShopMongoPlaywrightTest' test`.
- Result: failed as expected with 9 tests run, 4 failures, and 5 errors.
- Dependency convergence failed because the runtime Hibernate version is still `7.3.3.Final` instead of the MongoDB extension line `6.6.34.Final`.
- Boot, entity mapping, importer, HTTP, reporting, and Playwright tests failed while `HibernateSupport` still tried to create a PostgreSQL/JDBC environment for a Mongo replica-set URI.
- Browser ID handling failed because relationship IDs are still parsed through numeric helpers.
- Reporting source checks failed because the service still contains PostgreSQL-native SQL constructs such as `date_trunc`, `generate_series`, and `join sale_lines`.
