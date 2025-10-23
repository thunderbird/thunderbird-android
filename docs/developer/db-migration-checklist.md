# Database Migration Checklist

This document provides a checklist for developers and reviewers to ensure that database migrations are implemented
correctly, safely, and consistently across the project. Following these steps is mandatory for any change that alters
the database schema.

## Phase 1: Development

The developer implementing the migration is responsible for completing these steps.

### 1. Schema and Versioning:

- **Bump the Database Version:** The `DB_VERSION` constant in `legacy/storage/src/main/java/com/fsck/k9/storage/StoreSchemaDefinition.java` has been incremented by exactly **1**.
- **Update the database schema definition:** Any changes to the database schema have been reflected in the `dbCreateDatabaseFromScratch` within the `StoreSchemaDefinition.java` file.
- **Create a New Migration Class:** A new `MigrationToXX.kt` class file within the `legacy/storage/src/main/java/com/fsck/k9/storage/migrations` folder has been created, where `XX` is the new database version number.
- **Migration Logic Implemented:** The new migration class contains the necessary SQL statements to transition the database from the previous version to the new version.
- **Register the Migration:** The new migration class has been registered in the `Migrations.kt` file in the `legacy/storage/src/main/java/com/fsck/k9/storage/migrations` folder.

### 2. Migration Logic:

- **Data Integrity:** The migration logic has been designed to preserve existing data and ensure data integrity.
- **Idempotency:** The migration can be safely re-run without causing issues or data corruption.
- **Error Handling:** Appropriate error handling has been implemented to manage potential issues during the migration process.
- **No Network Calls:** The migration does not make any network calls. If network calls are absolutely necessary, they are handled gracefully and do not fail the migration.
- **Self-contained Logic:** The migration logic is self-contained within the migration class and does not depend on application logic outside of it.
- **Performance Considerations:** Long running migrations will block the app startup (a dedicated loading screen will be shown). Consider breaking them into smaller steps if necessary.
- **Documentation:** The migration class includes comments explaining the purpose of the migration and any non-obvious logic.

### 3. Testing:

- **Unit Tests:** Unit tests for the migration have been written to cover various scenarios, including edge cases.
- **Test Schema Changes:** The test validates that the schema is correct after the migration runs. It should check for:
  - New tables exist.
  - New columns exist in the correct tables.
  - Correct column types, nullability, and default values.
- **Test Data Migration:** The test validates that existing data is correctly migrated. This includes:
  - Data in existing tables remains intact.
  - Data transformations (if any) are correctly applied.

### 4. Holistic testing:

- **Fresh Install:** The app installs and runs correctly on a fresh install.
- **Upgrade from Production:** The app upgrades correctly from the latest production version.
- **Upgrade from Beta:** The app upgrades correctly from the latest beta version.

## Phase 2: Review

The reviewer is responsible for validating these steps during the code review process.

### 1. Code and Logic Review:

- **Verify Version Bump:** Confirm that the database version has been incremented correctly by 1.
- **Schema Definition Update:** Ensure that the database schema definition has been updated to reflect the new schema.
- **Review Migration Class:** Ensure the new migration class is correctly named and placed in the appropriate directory.
- **Validate Migration Logic:** Review the SQL statements in the migrate() method for correctness and safety.
- **Check for Data Integrity:** Ensure that the migration logic preserves existing data and does not introduce data loss unless explicitly intended.
- **Performance Review:** Assess the migration logic for potential performance bottlenecks, especially on large datasets.
- **Review Documentation:** Check that the migration class is well-commented, explaining the purpose and any complex logic.

### 2. Testing Review:

- **Confirm Unit Tests:** Ensure that unit tests for the migration have been written and cover various scenarios.
- **Review Test Coverage:** Validate that the tests adequately cover the schema changes and data migration paths, including edge cases.
- **Review Reordering Scenarios:** If the PR involves reordering, confirm that the developer has followed the renumbering protocol and re-verified their changes.

### 3. Holistic check:

- **Fresh Install:** The app installs and runs correctly on a fresh install.
- **Upgrade from Production:** The app upgrades correctly from the latest production version.
- **Upgrade from Beta:** The app upgrades correctly from the latest beta version.

## What to watch out for:

- **Data Loss:** Ensure that no unintended data loss occurs during the migration.
- **Network Calls:** Avoid making network calls during the migration process. If necessary, ensure they are handled gracefully and do not fail the migration.
- **Merge Conflicts on Uplift:** Be prepared for merge conflicts in `StoreSchemaDefinition.java` and `Migrations.kt` when rebasing. When resolving them, ensure you are correctly renumbering your migration and not overwriting someone else's.
- **Uplifting Hotfixes:** When uplifting a hotfix with a migration to a public branch (`beta`, `release`), its version number must be higher than the highest version across **all** branches (`main`, `beta`, `release`). See "Scenario B" for the detailed "jump over" strategy. Never rewrite the migration history of a public branch.
- **Write migrations that are self-contained and do not depend on application logic outside the migration class.**
- **Long-running Migrations:** Be cautious of migrations that may take a long time to complete, especially on large datasets. Consider breaking them into smaller steps if necessary.
- **Blocking the Main Thread:** Migrations run on the main thread and will block the UI. Keep migrations as fast as possible.

## Handling Rebases and Uplifts

When working with migrations, you'll often encounter situations where the order changes. Below are two common scenarios
and how to handle them.

### Scenario A: Rebasing a Feature Branch

This happens when you are about to merge your branch, but another migration has been merged into `main` in the meantime.

#### Step 1: Renumber Your Migration

- **Rebase Your Branch:** Before merging, always rebase your branch on the latest version of `main`.
- **Resolve Conflicts:** If another migration has taken your version number, you'll need to renumber your migration to the next available version number.
- **Rename and Update Files:**
  - Rename your `MigrationToXX.kt` file to `MigrationToYY.kt`, where `YY` is the new, higher version number.
  - Update the class name inside the file to match (e.g., `class MigrationToYY(...)`).
  - Update the `DB_VERSION` in `StoreSchemaDefinition.java` to `YY`.
  - Update the registration in `Migrations.kt` to use your new `MigrationToYY` class.

#### Step 2: Verify Your Renumbered Migration

Renumbering is changing the context of your migration, and you must verify its correctness again.

- **Verify Schema Definition:** Ensure that the `dbCreateDatabaseFromScratch` method in `StoreSchemaDefinition.java` reflects the correct final schema after all migrations, including your renumbered one.
- **Verify Migration Logic:** Your migration will now run *after* a different one. Review your SQL logic to ensure it's still valid. For example, if you are modifying a table that the new intermediate migration also touched, you must ensure your changes don't cause conflicts.
- **Re-run All Tests:** Thoroughly re-run all unit and integration tests to ensure your migration still works as expected in the new order.

### Scenario B: Uplifting a Hotfix to a Public Branch (`beta` or `release`)

This scenario is complex and requires extreme care. It occurs when a hotfix with a migration needs to be applied to
`release` and/or `beta`, while newer migrations may already exist on `beta`.

The goal is to release a hotfix without breaking any user's upgrade path, regardless of which track (release/beta) they
are on or switch to later.

#### What you must preserve

- **Always Upgrade:** A user's database version must only ever increase. No downgrades are allowed.
- **Public History is Immutable:** Never rewrite the public migration history once a migration has shipped in a public build (`beta`, `release`), you **must not** renumber, remove, or alter it.
- **Minimize Hotfix Migrations:** Avoid migrations in hotfixes if possible. If unavoidable, keep them minimal and fully self-contained.

#### A "How to uplift" guide

Warning: This is a delicate process and requires careful attention to detail.

1. Audit current versions:
   - `R` = latest version shipped on `release`
   - `B` = latest version shipped on `beta`
   - `M` = current version on `main` (where your hotfix branch is based)
2. Pick the hotfix version:
   - Set the hotfix migration version `H` to `max(R, B, M) + 1` (pick +2/+3 if multiple hotfixes are needed).
3. Create the hotfix migration on the target branch (`beta` or `release`):
   - Create `MigrationToH.kt` with your migration logic.
   - Update `DB_VERSION` in `StoreSchemaDefinition.java` to `H`.
   - Register the migration in `Migrations.kt`.
   - Update the definition in `dbCreateDatabaseFromScratch` to the post `H` schema.
4. Patch `beta` first, then `main` immediately after:
   - Set `DB_VERSION` in `StoreSchemaDefinition.java` on `beta`, `main` to `H`.
   - Register `MigrationToH` in `Migrations.kt` on both branches. Reuse the same migration.
   - Verify `dbCreateDatabaseFromScratch` on both branches reflects the post-`H` schema.
5. Reconcile any unreleased migrations below `H`:
   - For both `beta` and `main`, ensure any unreleased migrations with version `< H` are updated against the new schema after `H`.
   - This may involve reintroducing them as new migrations with versions `H+1`, `H+2`, etc., on both branches.
   - Verify `dbCreateDatabaseFromScratch` again to ensure it reflects the final schema after all migrations.

#### Example

Scenario: Uplift a hotfix migration `MigrationTo9.kt` from `main` to `beta`, while preserving existing migrations.

Initial state before uplift:

- `release` branch is at version `R=5`. The last shipped migration is `MigrationTo5.kt`
- `beta` branch is at version `B=7`. This includes `MigrationTo6.kt` and `MigrationTo7.kt`.
  - Let's assume a beta build with version `7` has not yet been shipped. But a version `6` has been shipped to beta users.
- `main` branch is at version `M=10`, it contains migrations up to `MigrationTo10.kt`.
- A critical bug requires a hotfix. The necessary migration logic currently exists on main as `MigrationTo9.kt`.

Goal: Release the logic from `MigrationTo9.kt` as a hotfix to beta users without disrupting the unreleased `MigrationTo7.kt`.

Steps:

1. Pick hotfix version -> `H=11`.
2. Create the Hotfix on a temporary branch based on `beta`:
   - On your new branch, create `MigrationTo11.kt`. Copy the logic from main's `MigrationTo9.kt` into this new file.
   - Review the logic, since it will now run after `MigrationTo7.kt`.
   - Update `DB_VERSION` in `StoreSchemaDefinition.java` to `11`.
   - Register `MigrationTo11.kt` in `Migrations.kt`.
   - Update `dbCreateDatabaseFromScratch` to include the schema changes from `MigrationTo7.kt` and `MigrationTo11.kt`.
   - Merge the hotfix to `beta`, which is now on version `11`.
3. Reconcile `main`:
   - The `main` branch has a more complex history (`MigrationTo8.kt`, `MigrationTo9.kt`, `MigrationTo10.kt`) that must be re-evaluated now that version `11` has been introduced.
   - The original `MigrationTo9.kt` on main is now redundant and its version number is obsolete.
   - Create a PR for `main` to:
     1. Remove obsolete `MigrationTo9.kt`.
     2. Add the same `MigrationTo11.kt` file that was used on the `beta` branch and register it in `Migrations.kt`.
     3. Renumber and re-introduce other migrations (`MigrationTo8.kt` as `MigrationTo12.kt`, `MigrationTo10.kt` as `MigrationTo13.kt`) to ensure continuity.
     4. Set the final `DB_VERSION` in `StoreSchemaDefinition.java` to `13`.
     5. Update `dbCreateDatabaseFromScratch` to reflect the final schema after all migrations.

Result:

- `release` branch remains at version `5`.
- `beta` ships version `11`.
- `main` branch is now at version  `13`.
- All user upgrade paths are preserved:
  - Release users on version 5 will not be affected by this hotfix. They will only upgrade when a new version is published to the release track.
  - Beta users on the pre-hotfix version `6` will upgrade directly to version `11`, correctly applying migrations `7` and `11`.
- The migration history is now linear and consistent across all branches, preventing future conflicts.

