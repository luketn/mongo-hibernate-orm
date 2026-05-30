# Step 14 Notes: Typed Aggregation Results

- Changed reporting aggregations to deserialize directly into the report API records with `aggregate(..., ResultType.class)`.
- Kept the existing DTO scale normalization in Java for money values, margins, and growth rates.
- Removed loose `Document` extraction from totals, weekly trends, best-seller insights, and forecasts.
- Kept retention on the existing separate-query path with Java aggregation, as previously agreed.
- Used Decimal128 constants in aggregation expressions so BigDecimal record fields receive compatible BSON values.
- Converted forecast projected units with `$toInt($ceil(...))` so the record codec decodes the result as an integer.
- Verified with `mvn-lt -Dtest=MongoReportingServiceTest,MongoReportingSourceTest test`.
- Verified with `mvn-lt test`; the full suite passed with 12 tests.
