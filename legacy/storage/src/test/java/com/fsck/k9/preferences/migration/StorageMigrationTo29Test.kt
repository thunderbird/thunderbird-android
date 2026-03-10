package com.fsck.k9.preferences.migration

import assertk.assertThat
import assertk.assertions.doesNotContainKey
import assertk.assertions.isEqualTo
import assertk.assertions.key
import com.fsck.k9.preferences.createPreferencesDatabase
import kotlin.test.Test
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.logging.testing.TestLogger
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StorageMigrationTo29Test {
    private val database = createPreferencesDatabase()
    private val migrationHelper = DefaultStorageMigrationHelper()
    private val migration = StorageMigrationTo29(database, migrationHelper)

    @Before
    fun setUp() {
        Log.logger = TestLogger()
    }

    @Test
    fun `migration should rename account_setup_auto_expand_folder to auto_select_folder`() {
        // Arrange: Insert old data into the database
        migrationHelper.insertValue(database, "account_setup_auto_expand_folder", "some value")

        // Act: Run the migration
        migration.renameAutoSelectFolderPreference()

        // Assert: Verify the results
        val values = migrationHelper.readAllValues(database)
        assertThat(values).key("auto_select_folder").isEqualTo("some value")
        assertThat(values).doesNotContainKey("account_setup_auto_expand_folder")
    }

    @After
    fun tearDown() {
        database.close()
    }
}
