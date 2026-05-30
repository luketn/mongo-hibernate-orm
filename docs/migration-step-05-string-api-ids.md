# Step 05 Notes: String API IDs

- Converted API view IDs and relationship request IDs from numeric `Long` values to opaque `String` values.
- Updated HTTP item routing to pass path ID segments through as strings instead of parsing them as longs.
- Updated service method signatures to accept string IDs while still parsing them to `Long` internally until the entity mapping steps convert persistence IDs to `ObjectId`.
- Updated browser payload construction so relationship IDs are submitted as strings; numeric parsing remains only for quantities, years, and decimal fields.
- Verified with `mvn-lt -Dtest=MongoBrowserIdHandlingTest test`.
