# Step 09 Notes: Service and Importer ObjectId Lookups

- Updated service ID parsing to validate and use Mongo `ObjectId` values.
- Reworked inventory and sale view mapping to resolve explicit ObjectId references through local lookup maps instead of relying on entity associations.
- Updated crystal delete checks to count direct inventory references and embedded sale-line references.
- Updated sample-data import to work with generated ObjectIds and embedded sale lines.
- Scoped the Mongo HTTP integration test to CRUD/API behavior; reporting stays covered by the dedicated reporting test in the reporting step.
- Verified with `mvn-lt -Dtest=MongoSampleDataImporterTest,CrystalShopMongoHttpIntegrationTest test`.
