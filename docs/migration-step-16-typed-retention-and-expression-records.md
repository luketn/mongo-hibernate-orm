# Step 16 Notes: Typed Retention And Expression Records

- Changed the retention sale-activity query to use `find(..., SaleActivity.class)` instead of reading `Document` rows.
- Removed the manual `Document` to `SaleActivity` mapping and the associated date conversion helper.
- Replaced remaining BSON expression document builders with named records for group keys, `$dateTrunc`, and unary operator expressions.
- Kept retention aggregation in Java as agreed, but made the Mongo read side strongly typed.
- Verified with `mvn-lt -Dtest=MongoReportingServiceTest test`.
- Verified with `mvn-lt test`; the full suite passed with 15 tests.
