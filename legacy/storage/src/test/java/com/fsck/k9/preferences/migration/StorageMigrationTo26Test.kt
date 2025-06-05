package com.fsck.k9.preferences.migration

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.key
import com.fsck.k9.preferences.createPreferencesDatabase
import java.util.UUID
import kotlin.test.Test
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.logging.testing.TestLogger
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StorageMigrationTo26Test {
    private val database = createPreferencesDatabase()
    private val migrationHelper = DefaultStorageMigrationHelper()
    private val migration = StorageMigrationTo26(database, migrationHelper)

    @Before
    fun setUp() {
        Log.logger = TestLogger()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `valid email addresses should not be changed`() {
        val accountOne = createAccount("email.0" to "one@domain.example")
        val accountTwo = createAccount("email.0" to "two@domain.example")
        writeAccountUuids(accountOne, accountTwo)

        migration.fixIdentities()

        assertThat(migrationHelper.readAllValues(database)).all {
            key("$accountOne.email.0").isEqualTo("one@domain.example")
            key("$accountTwo.email.0").isEqualTo("two@domain.example")
        }
    }

    @Test
    fun `valid email addresses with additional spaces should be trimmed`() {
        val account = createAccount(
            "email.0" to " valid@domain.example ",
        )
        writeAccountUuids(account)

        migration.fixIdentities()

        assertThat(migrationHelper.readAllValues(database))
            .key("$account.email.0").isEqualTo("valid@domain.example")
    }

    @Test
    fun `invalid email addresses should be replaced`() {
        val account = createAccount(
            "email.0" to "",
            "email.1" to "invalid",
        )
        writeAccountUuids(account)

        migration.fixIdentities()

        assertThat(migrationHelper.readAllValues(database)).all {
            key("$account.email.0").isEqualTo("please.edit@invalid")
            key("$account.email.1").isEqualTo("please.edit@invalid")
        }
    }

    private fun writeAccountUuids(vararg accounts: String) {
        val accountUuids = accounts.joinToString(separator = ",")
        migrationHelper.insertValue(database, "accountUuids", accountUuids)
    }

    private fun createAccount(vararg pairs: Pair<String, String>): String {
        val accountUuid = UUID.randomUUID().toString()

        for ((key, value) in pairs) {
            migrationHelper.insertValue(database, "$accountUuid.$key", value)
        }

        return accountUuid
    }
}
