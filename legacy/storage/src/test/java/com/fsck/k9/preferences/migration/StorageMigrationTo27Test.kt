package com.fsck.k9.preferences.migration

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.key
import com.fsck.k9.preferences.createPreferencesDatabase
import java.util.UUID
import kotlin.test.Test
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StorageMigrationTo27Test {
    private val database = createPreferencesDatabase()
    private val migrationHelper = DefaultStorageMigrationHelper()
    private val migration = StorageMigrationTo27(database, migrationHelper)

    @Before
    fun setUp() {
        Log.logger = TestLogger()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `avatar monogram should be added for accounts with MONOGRAM avatar type and no monogram, name and email`() {
        val accountUuid = createAccount(
            "avatarType" to AvatarTypeDto.MONOGRAM.name,
            "avatarMonogram" to null,
            "name.0" to null,
            "email.0" to null,
        )
        writeAccountUuids(accountUuid)

        migration.addAvatarMonogram()

        assertThat(migrationHelper.readAllValues(database))
            .key("$accountUuid.avatarMonogram").isEqualTo("XX")
    }

    @Test
    fun `avatar monogram should not be added for accounts with MONOGRAM avatar type and existing monogram`() {
        val accountUuid = createAccount(
            "avatarType" to AvatarTypeDto.MONOGRAM.name,
            "avatarMonogram" to "AB",
        )
        writeAccountUuids(accountUuid)

        migration.addAvatarMonogram()

        assertThat(migrationHelper.readAllValues(database))
            .key("$accountUuid.avatarMonogram").isEqualTo("AB")
    }

    @Test
    fun `avatar monogram should be added for accounts with name and no monogram`() {
        val accountUuid = createAccount(
            "avatarType" to AvatarTypeDto.MONOGRAM.name,
            "avatarMonogram" to null,
            "name.0" to "John Doe",
            "email.0" to "test@example.com",
        )
        writeAccountUuids(accountUuid)

        migration.addAvatarMonogram()

        assertThat(migrationHelper.readAllValues(database))
            .key("$accountUuid.avatarMonogram").isEqualTo("JO")
    }

    @Test
    fun `avatar monogram should be added for accounts with no name and monogram but email`() {
        val accountUuid = createAccount(
            "avatarType" to AvatarTypeDto.MONOGRAM.name,
            "avatarMonogram" to null,
            "name.0" to null,
            "email.0" to "test@example.com",
        )
        writeAccountUuids(accountUuid)

        migration.addAvatarMonogram()

        assertThat(migrationHelper.readAllValues(database))
            .key("$accountUuid.avatarMonogram").isEqualTo("TE")
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
