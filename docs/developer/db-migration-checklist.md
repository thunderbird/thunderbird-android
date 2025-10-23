# Database Migration Checklist

This document provides a checklist for developers and reviewers to ensure that database migrations are implemented
correctly, safely, and consistently across the project. Following these steps is mandatory for any change that alters
the database schema.

## Phase 1: Development

The developer implementing the migration is responsible for completing these steps.

### 1. Schema and Versioning:

- [ ] **Bump the Database Version:** The `DB_VERSION` constant in `legacy/storage/src/main/java/com/fsck/k9/storage/StoreSchemaDefinition.java` has been incremented by exactly **1**.
- [ ] **Update the database schema definition:** Any changes to the database schema have been reflected in the `dbCreateDatabaseFromScratch` within the `StoreSchemaDefinition.java` file.
- [ ] **Create a New Migration Class:** A new `MigrationToXX` class within the `legacy/storage/src/main/java/com/fsck/k9/storage/migrations` folder has been created. Where XX is the new database version number.
- [ ] **Migration Logic Implemented:** The new migration class contains the necessary SQL statements to transition the database from the previous version to the new version.
- [ ] **Register the Migration:** The new migration class has been registered in the `Migrations.kt` file in the `legacy/storage/src/main/java/com/fsck/k9/storage/migrations` folder.

### 2. Migration Logic:

- [ ] **Data Integrity:** The migration logic has been designed to preserve existing data and ensure data integrity.
- [ ] **Idempotency:** The migration can be safely re-run without causing issues or data corruption.
- [ ] **Error Handling:** Appropriate error handling has been implemented to manage potential issues during the migration process.
- [ ] **No Network Calls:** The migration does not make any network calls. If network calls are absolutely necessary, they are handled gracefully and do not fail the migration.
- [ ] **Self-contained Logic:** The migration logic is self-contained within the migration class and does not depend on application logic outside of it.
- [ ] **Performance Considerations:** Long running migrations will block the app startup (a dedicated loading screen will be shown). Consider breaking them into smaller steps if necessary.
- [ ] **Documentation:** The migration class includes comments explaining the purpose of the migration and any non-obvious logic.

### 3. Testing:

- [ ] **Unit Tests:** Unit tests for the migration have been written to cover various scenarios, including edge cases.
- [ ] **Test Schema Changes:** The test validates that the schema is correct after the migration runs. It should check for:
  - New tables exist.
  - New columns exist in the correct tables.
  - Correct column types, nullability, and default values.
- [ ] **Test Data Migration:** The test validates that existing data is correctly migrated. This includes:
  - Data in existing tables remains intact.
  - Data transformations (if any) are correctly applied.

### 4. Holistic testing:

- [ ] Run migration **Beta -> Beta** and verify no issues.
- [ ] Run migration **Beta -> Release** and verify no issues.
- [ ] Run migration **Release -> Release** and verify no issues.

## Phase 2: Review

The reviewer is responsible for validating these steps during the code review process.

### 1. Code and Logic Review:

- [ ] **Verify Version Bump:** Confirm that the database version has been incremented correctly by 1.
- [ ] **Schema Definition Update:** Ensure that the database schema definition has been updated to reflect the new schema.
- [ ] **Review Migration Class:** Ensure the new migration class is correctly named and placed in the appropriate directory.
- [ ] **Validate Migration Logic:** Review the SQL statements in the migrate() method for correctness and safety.
- [ ] **Check for Data Integrity:** Ensure that the migration logic preserves existing data and does not introduce data loss unless explicitly intended.
- [ ] **Performance Review:** Assess the migration logic for potential performance bottlenecks, especially on large datasets.
- [ ] **Review Documentation:** Check that the migration class is well-commented, explaining the purpose and any complex logic.

### 2. Testing Review:

- [ ] **Confirm Unit Tests:** Ensure that unit tests for the migration have been written and cover various scenarios.
- [ ] **Review Test Coverage:** Validate that the tests adequately cover the schema changes and data migration paths, including edge cases.

### 3. Holistic check:

- [ ] Run migration **Beta -> Beta** and verify no issues.
- [ ] Run migration **Beta -> Release** and verify no issues.
- [ ] Run migration **Release -> Release** and verify no issues.

## What to watch out for:

- [ ] **Data Loss:** Ensure that no unintended data loss occurs during the migration.
- [ ] **Network Calls:** Avoid making network calls during the migration process. If necessary, ensure they are handled gracefully and do not fail the migration.
- [ ] **Write migrations that are self-contained and do not depend on application logic outside the migration class.**
- [ ] **Long-running Migrations:** Be cautious of migrations that may take a long time to complete, especially on large datasets. Consider breaking them into smaller steps if necessary.
- [ ] **Blocking the Main Thread:** Migrations run on the main thread and will block the UI. Keep migrations as fast as possible.

