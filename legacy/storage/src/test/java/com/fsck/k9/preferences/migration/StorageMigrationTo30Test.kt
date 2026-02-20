package com.fsck.k9.preferences.migration

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.key
import com.fsck.k9.preferences.createPreferencesDatabase
import java.util.UUID
import kotlin.test.Test
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.feature.mail.folder.api.ArchiveGranularity
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
    fun `archiveGranularity should be set to SINGLE_ARCHIVE_FOLDER for existing accounts`() {
        val accountUuid = createAccount()
        writeAccountUuids(accountUuid)

        migration.setDefaultArchiveGranularity()

        assertThat(migrationHelper.readAllValues(database))
            .key("$accountUuid.archiveGranularity")
            .isEqualTo(ArchiveGranularity.MIGRATION_DEFAULT.name)
    }

    @Test
    fun `archiveGranularity should not overwrite existing value`() {
        val accountUuid = createAccount(
            "archiveGranularity" to ArchiveGranularity.PER_MONTH_ARCHIVE_FOLDERS.name,
        )
        writeAccountUuids(accountUuid)

        migration.setDefaultArchiveGranularity()

        assertThat(migrationHelper.readAllValues(database))
            .key("$accountUuid.archiveGranularity")
            .isEqualTo(ArchiveGranularity.PER_MONTH_ARCHIVE_FOLDERS.name)
    }

    @Test
    fun `archiveGranularity should be set for multiple accounts without the setting`() {
        val account1Uuid = createAccount()
        val account2Uuid = createAccount()
        val account3Uuid = createAccount()
        writeAccountUuids(account1Uuid, account2Uuid, account3Uuid)

        migration.setDefaultArchiveGranularity()

        val values = migrationHelper.readAllValues(database)
        assertThat(values)
            .key("$account1Uuid.archiveGranularity")
            .isEqualTo(ArchiveGranularity.MIGRATION_DEFAULT.name)
        assertThat(values)
            .key("$account2Uuid.archiveGranularity")
            .isEqualTo(ArchiveGranularity.MIGRATION_DEFAULT.name)
        assertThat(values)
            .key("$account3Uuid.archiveGranularity")
            .isEqualTo(ArchiveGranularity.MIGRATION_DEFAULT.name)
    }

    @Test
    fun `archiveGranularity should handle mix of accounts with and without existing values`() {
        val accountWithoutSetting = createAccount()
        val accountWithYearly = createAccount(
            "archiveGranularity" to ArchiveGranularity.PER_YEAR_ARCHIVE_FOLDERS.name,
        )
        val accountWithMonthly = createAccount(
            "archiveGranularity" to ArchiveGranularity.PER_MONTH_ARCHIVE_FOLDERS.name,
        )
        writeAccountUuids(accountWithoutSetting, accountWithYearly, accountWithMonthly)

        migration.setDefaultArchiveGranularity()

        val values = migrationHelper.readAllValues(database)
        assertThat(values)
            .key("$accountWithoutSetting.archiveGranularity")
            .isEqualTo(ArchiveGranularity.MIGRATION_DEFAULT.name)
        assertThat(values)
            .key("$accountWithYearly.archiveGranularity")
            .isEqualTo(ArchiveGranularity.PER_YEAR_ARCHIVE_FOLDERS.name)
        assertThat(values)
            .key("$accountWithMonthly.archiveGranularity")
            .isEqualTo(ArchiveGranularity.PER_MONTH_ARCHIVE_FOLDERS.name)
    }

    @Test
    fun `archiveGranularity should not be set when no accounts exist`() {
        writeAccountUuids()

        migration.setDefaultArchiveGranularity()

        val values = migrationHelper.readAllValues(database)
        assertThat(values.keys.filter { it.contains("archiveGranularity") }).isEqualTo(emptyList())
    }

    private fun writeAccountUuids(vararg accounts: String) {
        val accountUuids = accounts.joinToString(separator = ",")
        migrationHelper.insertValue(database, "accountUuids", accountUuids)
    }

    private fun createAccount(vararg pairs: Pair<String, String?>): String {
        val accountUuid = UUID.randomUUID().toString()

        for ((key, value) in pairs) {
            migrationHelper.insertValue(database, "$accountUuid.$key", value)
        }

        return accountUuid
    }
}
