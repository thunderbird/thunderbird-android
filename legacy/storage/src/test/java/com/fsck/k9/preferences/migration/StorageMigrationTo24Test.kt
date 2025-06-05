package com.fsck.k9.preferences.migration

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.key
import com.fsck.k9.preferences.createPreferencesDatabase
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.util.UUID
import kotlin.test.Test
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.logging.testing.TestLogger
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StorageMigrationTo24Test {
    private val database = createPreferencesDatabase()
    private val migrationHelper = DefaultStorageMigrationHelper()
    private val migration = StorageMigrationTo24(database, migrationHelper)

    @Before
    fun setUp() {
        Log.logger = TestLogger()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `AUTOMATIC with SSL_TLS_REQUIRED should be migrated to PLAIN`() {
        val account = createAccount(
            "outgoingServerSettings" to toJson(
                "type" to "smtp",
                "host" to "irrelevant.invalid",
                "port" to 465,
                "connectionSecurity" to "SSL_TLS_REQUIRED",
                "authenticationType" to "AUTOMATIC",
                "username" to "username",
                "password" to null,
                "clientCertificateAlias" to null,
            ),
        )
        writeAccountUuids(account)

        migration.removeLegacyAuthenticationModes()

        assertThat(migrationHelper.readAllValues(database))
            .key("$account.outgoingServerSettings")
            .isEqualTo(
                """
                {
                    "type": "smtp",
                    "host": "irrelevant.invalid",
                    "port": 465,
                    "connectionSecurity": "SSL_TLS_REQUIRED",
                    "authenticationType": "PLAIN",
                    "username": "username",
                    "password": null,
                    "clientCertificateAlias": null
                }
                """.toCompactJson(),
            )
    }

    @Test
    fun `AUTOMATIC with STARTTLS_REQUIRED should be migrated to PLAIN`() {
        val account = createAccount(
            "outgoingServerSettings" to toJson(
                "type" to "smtp",
                "host" to "irrelevant.invalid",
                "port" to 465,
                "connectionSecurity" to "STARTTLS_REQUIRED",
                "authenticationType" to "AUTOMATIC",
                "username" to "username",
                "password" to null,
                "clientCertificateAlias" to null,
            ),
        )
        writeAccountUuids(account)

        migration.removeLegacyAuthenticationModes()

        assertThat(migrationHelper.readAllValues(database))
            .key("$account.outgoingServerSettings")
            .isEqualTo(
                """
                {
                    "type": "smtp",
                    "host": "irrelevant.invalid",
                    "port": 465,
                    "connectionSecurity": "STARTTLS_REQUIRED",
                    "authenticationType": "PLAIN",
                    "username": "username",
                    "password": null,
                    "clientCertificateAlias": null
                }
                """.toCompactJson(),
            )
    }

    @Test
    fun `AUTOMATIC with NONE should be migrated to CRAM_MD5`() {
        val account = createAccount(
            "outgoingServerSettings" to toJson(
                "type" to "smtp",
                "host" to "irrelevant.invalid",
                "port" to 465,
                "connectionSecurity" to "NONE",
                "authenticationType" to "AUTOMATIC",
                "username" to "username",
                "password" to null,
                "clientCertificateAlias" to null,
            ),
        )
        writeAccountUuids(account)

        migration.removeLegacyAuthenticationModes()

        assertThat(migrationHelper.readAllValues(database))
            .key("$account.outgoingServerSettings")
            .isEqualTo(
                """
                {
                    "type": "smtp",
                    "host": "irrelevant.invalid",
                    "port": 465,
                    "connectionSecurity": "NONE",
                    "authenticationType": "CRAM_MD5",
                    "username": "username",
                    "password": null,
                    "clientCertificateAlias": null
                }
                """.toCompactJson(),
            )
    }

    @Test
    fun `LOGIN should be migrated to PLAIN`() {
        val accountOne = createAccount(
            "outgoingServerSettings" to toJson(
                "type" to "smtp",
                "host" to "irrelevant.invalid",
                "port" to 465,
                "connectionSecurity" to "SSL_TLS_REQUIRED",
                "authenticationType" to "LOGIN",
                "username" to "username",
                "password" to null,
                "clientCertificateAlias" to null,
            ),
        )
        val accountTwo = createAccount(
            "outgoingServerSettings" to toJson(
                "type" to "smtp",
                "host" to "another.irrelevant.invalid",
                "port" to 465,
                "connectionSecurity" to "STARTTLS_REQUIRED",
                "authenticationType" to "LOGIN",
                "username" to "user",
                "password" to "pass",
                "clientCertificateAlias" to null,
            ),
        )
        writeAccountUuids(accountOne, accountTwo)

        migration.removeLegacyAuthenticationModes()

        assertThat(migrationHelper.readAllValues(database)).all {
            key("$accountOne.outgoingServerSettings").isEqualTo(
                """
                {
                    "type": "smtp",
                    "host": "irrelevant.invalid",
                    "port": 465,
                    "connectionSecurity": "SSL_TLS_REQUIRED",
                    "authenticationType": "PLAIN",
                    "username": "username",
                    "password": null,
                    "clientCertificateAlias": null
                }
                """.toCompactJson(),
            )
            key("$accountTwo.outgoingServerSettings").isEqualTo(
                """
                {
                    "type": "smtp",
                    "host": "another.irrelevant.invalid",
                    "port": 465,
                    "connectionSecurity": "STARTTLS_REQUIRED",
                    "authenticationType": "PLAIN",
                    "username": "user",
                    "password": "pass",
                    "clientCertificateAlias": null
                }
                """.toCompactJson(),
            )
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

    private fun toJson(vararg pairs: Pair<String, Any?>): String {
        val moshi = Moshi.Builder().build()
        val adapter = moshi.adapter<Map<String, Any?>>(
            Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java),
        ).serializeNulls()

        return adapter.toJson(pairs.toMap()) ?: error("Failed to create JSON")
    }

    // Note: This only works for JSON strings where keys and values don't contain any spaces
    private fun String.toCompactJson(): String = replace(" ", "").replace("\n", "")
}
