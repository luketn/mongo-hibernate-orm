# Step 13 Notes: Aggregation Builder Syntax

- Kept report totals, weekly trends, product summaries, and forecasts inside MongoDB aggregation pipelines.
- Left customer retention as separate Mongo queries with Java aggregation, matching the agreed retention exception.
- Refactored reporting pipelines to use Mongo driver helpers from `Aggregates`, `Accumulators`, `Projections`, `Filters`, and `Sorts`.
- Used the driver's MQL expression API for report arithmetic, comparisons, date formatting, month extraction, and array sizing.
- Isolated raw `Document` construction to composite group-key assembly and the `$dateTrunc`/`$ceil` expression wrappers that are not covered by the driver helper API in use here.
- Verified with `mvn-lt -Dtest=MongoReportingServiceTest,MongoReportingSourceTest test`.
- Verified with `mvn-lt test`; the full suite passed with 12 tests.
