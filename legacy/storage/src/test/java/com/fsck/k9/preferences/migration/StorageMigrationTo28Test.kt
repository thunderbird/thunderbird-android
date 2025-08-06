package com.fsck.k9.preferences.migration

import assertk.assertThat
import assertk.assertions.doesNotContainKey
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
class StorageMigrationTo28Test {
    private val database = createPreferencesDatabase()
    private val migrationHelper = DefaultStorageMigrationHelper()
    private val migration = StorageMigrationTo28(database, migrationHelper)

    @Before
    fun setUp() {
        Log.logger = TestLogger()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `avatar type should be set for accounts with no avatar type`() {
        val accountUuid = createAccount(
            "avatarType" to null,
            "avatarMonogram" to "AB",
        )
        writeAccountUuids(accountUuid)

        migration.ensureAvatarSet()

        assertAvatarType(accountUuid, AvatarTypeDto.MONOGRAM)
        assertAvatarMonogram(accountUuid, "AB")
    }

    @Test
    fun `avatar type should not be set for accounts with existing avatar type`() {
        val accountUuid = createAccount(
            "avatarType" to AvatarTypeDto.IMAGE.name,
        )
        writeAccountUuids(accountUuid)

        migration.ensureAvatarSet()

        assertAvatarType(accountUuid, AvatarTypeDto.IMAGE)
        assertThat(migrationHelper.readAllValues(database))
            .doesNotContainKey("$accountUuid.avatarMonogram")
    }

    @Test
    fun `avatar monogram should be set for accounts with MONOGRAM avatar type and no monogram`() {
        val accountUuid = createAccount(
            "avatarType" to AvatarTypeDto.MONOGRAM.name,
            "avatarMonogram" to null,
            "name.0" to "John Doe",
            "email.0" to "test@example.com",
        )
        writeAccountUuids(accountUuid)

        migration.ensureAvatarSet()

        assertAvatarType(accountUuid, AvatarTypeDto.MONOGRAM)
        assertAvatarMonogram(accountUuid, "JO")
    }

    @Test
    fun `avatar monogram should be added for accounts with no monogram and name but email`() {
        val accountUuid = createAccount(
            "avatarType" to AvatarTypeDto.MONOGRAM.name,
            "avatarMonogram" to null,
            "name.0" to null,
            "email.0" to "test@example.com",
        )
        writeAccountUuids(accountUuid)

        migration.ensureAvatarSet()

        assertAvatarType(accountUuid, AvatarTypeDto.MONOGRAM)
        assertAvatarMonogram(accountUuid, "TE")
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

        migration.ensureAvatarSet()

        assertAvatarType(accountUuid, AvatarTypeDto.MONOGRAM)
        assertAvatarMonogram(accountUuid, "XX")
    }

    @Test
    fun `avatar type and monogram should be set for accounts with missing avatar type and monogram`() {
        val accountUuid = createAccount(
            "avatarType" to null,
            "avatarMonogram" to null,
            "name.0" to "John Doe",
            "email.0" to "test@example.com",
        )

        writeAccountUuids(accountUuid)

        migration.ensureAvatarSet()

        assertAvatarType(accountUuid, AvatarTypeDto.MONOGRAM)
        assertAvatarMonogram(accountUuid, "JO")
    }

    private fun assertAvatarType(
        accountUuid: String,
        expectedAvatarType: AvatarTypeDto,
    ) {
        assertThat(migrationHelper.readAllValues(database))
            .key("$accountUuid.avatarType").isEqualTo(expectedAvatarType.name)
    }

    private fun assertAvatarMonogram(
        accountUuid: String,
        expectedMonogram: String,
    ) {
        assertThat(migrationHelper.readAllValues(database))
            .key("$accountUuid.avatarMonogram").isEqualTo(expectedMonogram)
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
