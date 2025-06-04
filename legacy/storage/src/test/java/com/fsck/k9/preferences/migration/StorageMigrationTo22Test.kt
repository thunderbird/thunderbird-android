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
class StorageMigrationTo22Test {
    private val database = createPreferencesDatabase()
    private val migrationHelper = DefaultStorageMigrationHelper()
    private val migration = StorageMigrationTo22(database, migrationHelper)

    @Before
    fun setUp() {
        Log.logger = TestLogger()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    @Suppress("LongMethod")
    fun `fixServerSettings() should retain values while removing line breaks from username and password`() {
        val accountOne = createAccount(
            "incomingServerSettings" to toJson(
                "type" to "imap",
                "host" to "irrelevant.invalid",
                "port" to 993,
                "connectionSecurity" to "SSL_TLS_REQUIRED",
                "authenticationType" to "PLAIN",
                "username" to "user\n",
                "password" to "pass\nword",
                "clientCertificateAlias" to null,
            ),
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
        val accountTwo = createAccount(
            "incomingServerSettings" to toJson(
                "type" to "imap",
                "host" to "irrelevant.test",
                "port" to 143,
                "connectionSecurity" to "NONE",
                "authenticationType" to "XOAUTH2",
                "username" to "user@domain.example\r\n",
                "password" to null,
                "clientCertificateAlias" to null,
            ),
            "outgoingServerSettings" to toJson(
                "type" to "smtp",
                "host" to "irrelevant.test",
                "port" to 587,
                "connectionSecurity" to "STARTTLS_REQUIRED",
                "authenticationType" to "CRAM_MD5",
                "username" to "username",
                "password" to "password",
                "clientCertificateAlias" to "not-null",
            ),
        )
        writeAccountUuids(accountOne, accountTwo)

        migration.fixServerSettings()

        assertThat(migrationHelper.readAllValues(database)).all {
            key("$accountOne.incomingServerSettings").isEqualTo(
                """
                {
                    "type": "imap",
                    "host": "irrelevant.invalid",
                    "port": 993,
                    "connectionSecurity": "SSL_TLS_REQUIRED",
                    "authenticationType": "PLAIN",
                    "username": "user",
                    "password": "password",
                    "clientCertificateAlias": null
                }
                """.toCompactJson(),
            )
            key("$accountOne.outgoingServerSettings").isEqualTo(
                """
                {
                    "type": "smtp",
                    "host": "irrelevant.invalid",
                    "port": 465,
                    "connectionSecurity": "SSL_TLS_REQUIRED",
                    "authenticationType": "PLAIN",
                    "username": "",
                    "password": null,
                    "clientCertificateAlias": null
                }
                """.toCompactJson(),
            )

            key("$accountTwo.incomingServerSettings").isEqualTo(
                """
                {
                    "type": "imap",
                    "host": "irrelevant.test",
                    "port": 143,
                    "connectionSecurity": "NONE",
                    "authenticationType": "XOAUTH2",
                    "username": "user@domain.example",
                    "password": null,
                    "clientCertificateAlias": null
                }
                """.toCompactJson(),
            )
            key("$accountTwo.outgoingServerSettings").isEqualTo(
                """
                {
                    "type": "smtp",
                    "host": "irrelevant.test",
                    "port": 587,
                    "connectionSecurity": "STARTTLS_REQUIRED",
                    "authenticationType": "CRAM_MD5",
                    "username": "username",
                    "password": "password",
                    "clientCertificateAlias": "not-null"
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
