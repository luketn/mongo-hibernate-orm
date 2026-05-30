# Step 15 Notes: Report Totals Zero And Scale

- Removed the extra `ReportTotals` reconstruction after typed aggregation deserialization.
- Added `ReportTotals.ZERO` as the null-result fallback for empty annual totals.
- Added a Mongo record-codec test showing two-decimal `BigDecimal` values survive insert and read-back unchanged.
- Added a reporting aggregation test showing annual totals deserialize with two-decimal money scale.
- Added a no-sales report test showing empty totals return `ReportTotals.ZERO`.
- Verified with `mvn-lt -Dtest=MongoReportingServiceTest test`.
- Verified with `mvn-lt test`; the full suite passed with 15 tests.
