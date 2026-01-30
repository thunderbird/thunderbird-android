# Preference Migration Guide

This document explains how to update and migrate preferences in Thunderbird for Android.

## Overview

Thunderbird for Android uses a dedicated SQLite database to store its preferences as key-value pairs.
Both the database file and the primary table are named `preferences_storage`. This is managed by the
`K9StoragePersister`.

- **Database Name:** `preferences_storage`
- **Storage Table:** `preferences_storage`
- **Columns:** `primkey` (TEXT), `value` (TEXT)
- **Persister:** `legacy/storage/src/main/java/com/fsck/k9/preferences/K9StoragePersister.java`
- **Migration Manager:** `legacy/storage/src/main/java/com/fsck/k9/preferences/migration/StorageMigrations.kt`

## When to use a Preference Migration

You must implement a preference migration whenever you:

- Rename a preference key.
- Change the format of a preference value.
- Move a preference to a different storage location.
- Delete an obsolete preference.

## Adding a New Preference

If you are adding a completely new preference, you don't necessarily need a migration unless you need to populate it
with a default value that depends on other existing settings.

To add a new preference:

1. Define the key in the relevant component.
2. Use the `GeneralSettingsManager` or a specific preference manager to read and write the value.
3. If using the legacy `Storage` system directly, use `StorageEditor` to save the value.

> [!NOTE]
> If the new preference requires a default value that depends on existing settings, you must follow the
> [migration guide](#step-by-step-guide-for-migrations) below and bump the database version.

```kotlin
val editor = preferences.createStorageEditor()
editor.putBoolean("my_new_preference", true)
editor.commit()
```

## Step-by-Step Guide for Migrations

### 1. Bump the Preference Database Version

The `DB_VERSION` constant in `K9StoragePersister.java` must be incremented by exactly **1**. This version increase is what triggers the migration process.

```java
// legacy/storage/src/main/java/com/fsck/k9/preferences/K9StoragePersister.java
public class K9StoragePersister implements StoragePersister {
    private static final int DB_VERSION = 29; // Increment this
    // ...
}
```

### 2. Create a New Migration Class

Create a new Kotlin class in `legacy/storage/src/main/java/com/fsck/k9/preferences/migration/` named `StorageMigrationToXX.kt`, where `XX` is the new version number.

Use the `StorageMigrationHelper` to interact with the database.

```kotlin
/**
 * Describe the purpose of the migration here.
 */
class StorageMigrationToXX(
    private val db: SQLiteDatabase,
    private val migrationsHelper: StorageMigrationHelper,
) {
    fun runMigration() {
        val oldValue = migrationsHelper.readValue(db, "old_key")
        if (oldValue != null) {
            // Perform transformation if needed
            migrationsHelper.insertValue(db, "new_key", oldValue)
            migrationsHelper.writeValue(db, "old_key", null) // Deletes old_key
        }
    }
}
```

### 3. Register the Migration

Add the new migration to the `StorageMigrations.upgradeDatabase()` method.

```kotlin
// legacy/storage/src/main/java/com/fsck/k9/preferences/migration/StorageMigrations.kt
internal object StorageMigrations {
    @JvmStatic
    fun upgradeDatabase(db: SQLiteDatabase, migrationsHelper: StorageMigrationHelper) {
        val oldVersion = db.version

        // ... existing migrations ...
        if (oldVersion < XX) StorageMigrationToXX(db, migrationsHelper).runMigration()
    }
}
```

## Requirements

- **No Application Logic:** Do not use `Preferences` or other high-level application classes inside migrations, as their behavior might change over time. Use `StorageMigrationHelper` for direct database access.
- **No Network Calls:** Do not perform network calls during the migration, as they may fail due to external factors. In case it is really necessary, properly handle network failures to avoid data wipeout.
- **Testing:** Always write unit tests for your migrations. See the [Writing Unit Tests](#writing-unit-tests) section for details.
- **Idempotency:** Migrations should be safe to run multiple times, although the framework typically ensures they run only once.
- **Self-contained:** Keep the migration logic simple and contained within its class.

## Writing Unit Tests

Writing unit tests for preference migrations is mandatory to ensure data integrity and prevent regressions. These tests use Robolectric to provide a realistic Android environment.

### Test Setup

Your test class should be located in `legacy/storage/src/test/java/com/fsck/k9/preferences/migration/` and named
`StorageMigrationToXXTest.kt`.

Basic template for a migration test:

```kotlin
@RunWith(RobolectricTestRunner::class)
class StorageMigrationToXXTest {
    private val database = createPreferencesDatabase()
    private val migrationHelper = DefaultStorageMigrationHelper()
    private val migration = StorageMigrationToXX(database, migrationHelper)

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `migration should rename old_key to new_key`() {
        // Arrange: Insert old data into the database
        migrationHelper.insertValue(database, "old_key", "some value")

        // Act: Run the migration
        migration.runMigration()

        // Assert: Verify the results
        val values = migrationHelper.readAllValues(database)
        assertThat(values).key("new_key").isEqualTo("some value")
        assertThat(values).doesNotContainKey("old_key")
    }
}
```

### Key Considerations for Testing

- **Initial State:** Always start with a known state by inserting the necessary keys and values into the database using `migrationHelper.insertValue()`.
- **Edge Cases:** Test scenarios where keys might be missing, have unexpected values, or are in an old format.
- **Data Integrity:** Verify that values are correctly transformed if the migration involves format changes.
- **Cleanup:** Ensure the database is closed in the `@After` method to avoid resource leaks between tests.

## Common Tasks

### Renaming a Key

Use `readValue` to get the old value, `insertValue` to set the new key, and `writeValue(db, key, null)` to remove the old key.

### Account-specific Preferences

Account preferences are prefixed with the account UUID. You can find all account UUIDs by reading the `accountUuids` key.

```kotlin
val accountUuids = migrationsHelper.readValue(db, "accountUuids")?.split(",") ?: emptyList()
for (uuid in accountUuids) {
    val key = "$uuid.some_preference"
    // ...
}
```

### Deleting Obsolete Keys

Simply use `migrationsHelper.writeValue(db, "obsolete_key", null)`.
