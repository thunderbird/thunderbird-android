package com.fsck.k9.preferences.migration

import assertk.assertThat
import assertk.assertions.doesNotContainKey
import assertk.assertions.isEmpty
import assertk.assertions.isNotEmpty
import assertk.assertions.key
import com.fsck.k9.preferences.createPreferencesDatabase
import kotlin.test.Test
import net.thunderbird.components.core.logging.testing.TestLogger
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StorageMigrationTo32Test {

    private val logger = TestLogger()
    private val database = createPreferencesDatabase()
    private val migrationHelper = DefaultStorageMigrationHelper(logger)
    private val migration = StorageMigrationTo32(db = database, migrationsHelper = migrationHelper)

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `should reset old donation appeal values`() {
        migrationHelper.insertValue(database, "fundingReminderReferenceTimestamp", "1000")
        migrationHelper.insertValue(database, "fundingReminderShownTimestamp", "1000")
        migrationHelper.insertValue(database, "fundingActivityCounterInMillis", "1000")

        assertThat(migrationHelper.readAllValues(database)).isNotEmpty()

        migration.migrateK9FundingRemoval()

        val allValues = migrationHelper.readAllValues(database)

        assertThat(allValues).isEmpty()
        assertThat(allValues).doesNotContainKey("fundingReminderReferenceTimestamp")
        assertThat(allValues).doesNotContainKey("fundingReminderShownTimestamp")
        assertThat(allValues).doesNotContainKey("fundingActivityCounterInMillis")
    }

}
