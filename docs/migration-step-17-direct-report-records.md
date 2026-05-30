# Step 17 Notes: Direct Report Records

- Removed Java-side wrapper methods that rebuilt `WeeklySalesTrend`, `ProductSalesInsight`, and `ProductForecast` after typed Mongo deserialization.
- Returned typed aggregation result records directly from weekly trends, best sellers, and forecasts.
- Moved product margin rounding into the Mongo aggregation expression so the result record already carries the expected four-decimal value.
- Expanded reporting tests to assert money values keep two-decimal scale and ratio values keep four-decimal scale after aggregation deserialization.
- Verified with `mvn-lt -Dtest=MongoReportingServiceTest test`.
- Verified with `mvn-lt test`; the full suite passed with 15 tests.
