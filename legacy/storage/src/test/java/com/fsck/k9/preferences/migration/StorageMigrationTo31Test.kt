package com.fsck.k9.preferences.migration

import assertk.assertThat
import assertk.assertions.doesNotContainKey
import assertk.assertions.isEqualTo
import assertk.assertions.key
import com.fsck.k9.preferences.createPreferencesDatabase
import kotlin.test.Test
import net.thunderbird.components.core.logging.testing.TestLogger
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StorageMigrationTo31Test {
    private val logger = TestLogger()
    private val database = createPreferencesDatabase()
    private val migrationHelper = DefaultStorageMigrationHelper(logger)
    private val migration = StorageMigrationTo31(database, migrationHelper)

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `migration should convert TEXT with messageFormatAuto=true to AUTO`() {
        insertAccount(ACCOUNT_UUID_1, messageFormat = "TEXT", messageFormatAuto = "true")

        migration.migrateMessageFormatAuto()

        val values = migrationHelper.readAllValues(database)
        assertThat(values).key("$ACCOUNT_UUID_1.messageFormat").isEqualTo("AUTO")
        assertThat(values).doesNotContainKey("$ACCOUNT_UUID_1.messageFormatAuto")
    }

    @Test
    fun `migration should keep TEXT with messageFormatAuto=false`() {
        insertAccount(ACCOUNT_UUID_1, messageFormat = "TEXT", messageFormatAuto = "false")

        migration.migrateMessageFormatAuto()

        val values = migrationHelper.readAllValues(database)
        assertThat(values).key("$ACCOUNT_UUID_1.messageFormat").isEqualTo("TEXT")
        assertThat(values).doesNotContainKey("$ACCOUNT_UUID_1.messageFormatAuto")
    }

    @Test
    fun `migration should keep HTML even if messageFormatAuto=true`() {
        insertAccount(ACCOUNT_UUID_1, messageFormat = "HTML", messageFormatAuto = "true")

        migration.migrateMessageFormatAuto()

        val values = migrationHelper.readAllValues(database)
        assertThat(values).key("$ACCOUNT_UUID_1.messageFormat").isEqualTo("HTML")
        assertThat(values).doesNotContainKey("$ACCOUNT_UUID_1.messageFormatAuto")
    }

    @Test
    fun `migration should not insert messageFormat when it is missing`() {
        insertAccount(ACCOUNT_UUID_1, messageFormat = null, messageFormatAuto = "true")

        migration.migrateMessageFormatAuto()

        val values = migrationHelper.readAllValues(database)
        assertThat(values).doesNotContainKey("$ACCOUNT_UUID_1.messageFormat")
        assertThat(values).doesNotContainKey("$ACCOUNT_UUID_1.messageFormatAuto")
    }

    @Test
    fun `migration should migrate every account`() {
        insertAccount(ACCOUNT_UUID_1, messageFormat = "TEXT", messageFormatAuto = "true")
        insertAccount(ACCOUNT_UUID_2, messageFormat = "TEXT", messageFormatAuto = "false")
        migrationHelper.writeValue(database, "accountUuids", "$ACCOUNT_UUID_1,$ACCOUNT_UUID_2")

        migration.migrateMessageFormatAuto()

        val values = migrationHelper.readAllValues(database)
        assertThat(values).key("$ACCOUNT_UUID_1.messageFormat").isEqualTo("AUTO")
        assertThat(values).key("$ACCOUNT_UUID_2.messageFormat").isEqualTo("TEXT")
        assertThat(values).doesNotContainKey("$ACCOUNT_UUID_1.messageFormatAuto")
        assertThat(values).doesNotContainKey("$ACCOUNT_UUID_2.messageFormatAuto")
    }

    @Test
    fun `migration should do nothing without accounts`() {
        migration.migrateMessageFormatAuto()

        assertThat(migrationHelper.readAllValues(database)).isEqualTo(emptyMap())
    }

    private fun insertAccount(accountUuid: String, messageFormat: String?, messageFormatAuto: String) {
        if (migrationHelper.readValue(database, "accountUuids") == null) {
            migrationHelper.insertValue(database, "accountUuids", accountUuid)
        }
        messageFormat?.let { migrationHelper.insertValue(database, "$accountUuid.messageFormat", it) }
        migrationHelper.insertValue(database, "$accountUuid.messageFormatAuto", messageFormatAuto)
    }

    private companion object {
        const val ACCOUNT_UUID_1 = "00000000-0000-4000-0000-000000000001"
        const val ACCOUNT_UUID_2 = "00000000-0000-4000-0000-000000000002"
    }
}
