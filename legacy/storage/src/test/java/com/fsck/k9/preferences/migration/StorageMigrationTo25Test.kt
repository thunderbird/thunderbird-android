package com.fsck.k9.preferences.migration

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.key
import com.fsck.k9.preferences.createPreferencesDatabase
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.util.UUID
import kotlin.test.Test
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StorageMigrationTo25Test {
    private val database = createPreferencesDatabase()
    private val migrationHelper = DefaultStorageMigrationHelper()
    private val migration = StorageMigrationTo25(database, migrationHelper)

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `outgoing server settings with empty username should be migrated to use authenticationType = NONE`() {
        val account = createAccount(
            "outgoingServerSettings" to toJson(
                "type" to "smtp",
                "host" to "irrelevant.invalid",
                "port" to 465,
                "connectionSecurity" to "SSL_TLS_REQUIRED",
                "authenticationType" to "PLAIN",
                "username" to "",
                "password" to null,
                "clientCertificateAlias" to null,
            ),
        )
        writeAccountUuids(account)

        migration.convertToAuthTypeNone()

        assertThat(migrationHelper.readAllValues(database))
            .key("$account.outgoingServerSettings")
            .isEqualTo(
                """
                {
                    "type": "smtp",
                    "host": "irrelevant.invalid",
                    "port": 465,
                    "connectionSecurity": "SSL_TLS_REQUIRED",
                    "authenticationType": "NONE",
                    "username": "",
                    "password": null,
                    "clientCertificateAlias": null
                }
                """.toCompactJson(),
            )
    }

    @Test
    fun `outgoing server settings with username and no password should not be modified`() {
        val account = createAccount(
            "outgoingServerSettings" to toJson(
                "type" to "smtp",
                "host" to "irrelevant.invalid",
                "port" to 465,
                "connectionSecurity" to "SSL_TLS_REQUIRED",
                "authenticationType" to "PLAIN",
                "username" to "user",
                "password" to null,
                "clientCertificateAlias" to null,
            ),
        )
        writeAccountUuids(account)

        migration.convertToAuthTypeNone()

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
                    "username": "user",
                    "password": null,
                    "clientCertificateAlias": null
                }
                """.toCompactJson(),
            )
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
