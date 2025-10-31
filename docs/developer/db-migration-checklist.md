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

Hotfix migration uplift to `beta`:

- Current versions: `R=5`, `B=7`, `M=10`
- Last shipped beta migration: `MigrationTo6.kt`
- Last migration on beta: `MigrationTo7.kt`
- Migration to uplift: `MigrationTo9.kt`
- Hotfix migration version: `H = max(5, 7, 10) + 1 = 11`

Steps:

1. Pick hotfix version `H=11`.
2. On `beta` branch:
   - Create `MigrationTo11.kt` with hotfix logic, bump `DB_VERSION` to `11`, register it.
   - Uplift `MigrationTo9.kt` changes into `MigrationTo11.kt`.
   - Update `dbCreateDatabaseFromScratch` to reflect post-`11` schema.
   - Ship hotfix to `beta`.
3. Patch `beta` branch immediately after shipping (`H=11`):
   - Take the unreleased `MigrationTo7.kt` and reintroduce it as `MigrationTo12.kt`, adjusted against the new base schema (post‑`11`).
   - Bump `DB_VERSION` to `12`, register `MigrationTo12.kt`.
   - Update `dbCreateDatabaseFromScratch` to reflect post-`12` schema.
   - Remove old `MigrationTo7.kt` from `beta`.
4. Patch `main` branch immediately after shipping (`H=11`):
   1. Bump `DB_VERSION` to `11`, register `MigrationTo11.kt`.
   2. Update `dbCreateDatabaseFromScratch` to reflect post-`11` schema.
   3. Remove `MigrationTo9.kt` from `main`.
   4. Reintroduce `MigrationTo7.kt` as `MigrationTo12.kt`, bump `DB_VERSION` to `12`, register it.
   5. Update `dbCreateDatabaseFromScratch` to reflect post-`12` schema.
   6. If additional unreleased migrations exist, repeat the renumber-and-reverify process incrementally for any unreleased migrations after `12` (former `7`) as needed.

Result:

- `release` branch remains at `5`.
- `beta` ships `11`, then the branch advances to `12` (renumbered former `7`) for the next beta cycle.
- `main` branch updates to `≥12` (`11` plus any renumbered migrations).
- Upgrade paths:
  - Release users will upgrade from `5` to `11`, applying `MigrationTo6.kt` and `MigrationTo11.kt`.
  - Beta users will upgrade from `6` to `11`, applying `MigrationTo11.kt` only.

