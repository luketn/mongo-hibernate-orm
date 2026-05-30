# Step 07 Notes: Inventory ObjectId References

- Converted `InventoryItem` to a Mongo document in the `inventory` collection.
- Replaced `@ManyToOne` store/crystal relationships with explicit `ObjectId storeId` and `ObjectId crystalId` fields.
- Added a focused Mongo mapping test that verifies inventory documents store ObjectId references without embedding relational objects.
- Left temporary transient relationship accessors for service compatibility until the service/view mapping step.
- Verified with `mvn-lt -Dtest=MongoInventoryItemMappingTest test`.
