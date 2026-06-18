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
class StorageMigrationTo30Test {
    private val database = createPreferencesDatabase()
    private val migrationHelper = DefaultStorageMigrationHelper()
    private val migration = StorageMigrationTo30(database, migrationHelper)

    @Before
    fun setUp() {
        Log.logger = TestLogger()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `migration should convert animations=true to ON`() {
        migrationHelper.insertValue(database, ANIMATIONS_KEY, "true")

        migration.migrateAnimationSetting()

        assertThat(migrationHelper.readAllValues(database)).key(ANIMATIONS_KEY).isEqualTo("ON")
    }

    @Test
    fun `migration should convert animations=false to OFF`() {
        migrationHelper.insertValue(database, ANIMATIONS_KEY, "false")

        migration.migrateAnimationSetting()

        assertThat(migrationHelper.readAllValues(database)).key(ANIMATIONS_KEY).isEqualTo("OFF")
    }

    @Test
    fun `migration should not insert animations key when missing`() {
        migration.migrateAnimationSetting()

        assertThat(migrationHelper.readAllValues(database)).doesNotContainKey(ANIMATIONS_KEY)
    }

    @Test
    fun `migration should leave already migrated ON value unchanged`() {
        migrationHelper.insertValue(database, ANIMATIONS_KEY, "ON")

        migration.migrateAnimationSetting()

        assertThat(migrationHelper.readAllValues(database)).key(ANIMATIONS_KEY).isEqualTo("ON")
    }

    @Test
    fun `migration should leave already migrated OFF value unchanged`() {
        migrationHelper.insertValue(database, ANIMATIONS_KEY, "OFF")

        migration.migrateAnimationSetting()

        assertThat(migrationHelper.readAllValues(database)).key(ANIMATIONS_KEY).isEqualTo("OFF")
    }

    private companion object {
        const val ANIMATIONS_KEY = "animations"
    }
}
