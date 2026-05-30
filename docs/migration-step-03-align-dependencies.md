# Step 03 Notes: Align Dependencies

- Downgraded the explicit `hibernate-core` pin from `7.3.3.Final` to `6.6.34.Final`.
- Kept `org.mongodb:mongodb-hibernate:1.0.0-alpha0` and `org.testcontainers:mongodb` from the red-test step.
- The goal for this step is a single Hibernate ORM runtime line matching the published MongoDB Hibernate extension POM.
- Verified with `mvn-lt -Dtest=MongoDependencyConvergenceTest test`.
- Verified with `mvn dependency:tree -Dincludes=org.hibernate.orm:hibernate-core`; only `org.hibernate.orm:hibernate-core:jar:6.6.34.Final:compile` is present.
