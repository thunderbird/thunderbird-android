package net.thunderbird.feature.account.storage.legacy

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import kotlin.test.Test
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.logging.LogMessage
import net.thunderbird.core.logging.LogTag
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.account.storage.legacy.fake.FakeStorage
import net.thunderbird.feature.account.storage.legacy.fake.FakeStorageEditor
import net.thunderbird.feature.account.storage.legacy.serializer.ServerSettingsDtoSerializer

class LegacyAccountStorageHandlerTest {
    private val serverSettingsSerializer = ServerSettingsDtoSerializer()
    private val testSubject = LegacyAccountStorageHandler(
        serverSettingsDtoSerializer = serverSettingsSerializer,
        profileDtoStorageHandler = LegacyProfileDtoStorageHandler(LegacyAvatarDtoStorageHandler()),
        logger = NO_OP_LOGGER,
    )

    @Test
    fun `load should read use recipient address for reply`() {
        val account = createAccount()
        val storage = createStorage(mapOf("$ACCOUNT_UUID.useRecipientAddressForReply" to "true"))

        testSubject.load(account, storage)

        assertThat(account.useRecipientAddressForReply).isEqualTo(true)
    }

    @Test
    fun `load should default use recipient address for reply to false`() {
        val account = createAccount().apply { useRecipientAddressForReply = true }

        testSubject.load(account, createStorage())

        assertThat(account.useRecipientAddressForReply).isEqualTo(false)
    }

    @Test
    fun `save should store use recipient address for reply`() {
        val account = createAccount().apply { useRecipientAddressForReply = true }
        val editor = FakeStorageEditor()

        testSubject.save(account, FakeStorage(), editor)

        assertThat(editor.values["$ACCOUNT_UUID.useRecipientAddressForReply"]).isEqualTo("true")
    }

    @Test
    fun `delete should remove use recipient address for reply`() {
        val account = createAccount()
        val editor = FakeStorageEditor()

        testSubject.delete(account, FakeStorage(), editor)

        assertThat(editor.removedKeys).contains("$ACCOUNT_UUID.useRecipientAddressForReply")
    }

    private fun createStorage(additionalValues: Map<String, String> = emptyMap()): FakeStorage {
        val serializedSettings = serverSettingsSerializer.serialize(SERVER_SETTINGS)
        return FakeStorage(
            mapOf(
                "$ACCOUNT_UUID.incomingServerSettings" to serializedSettings,
                "$ACCOUNT_UUID.outgoingServerSettings" to serializedSettings,
                "$ACCOUNT_UUID.email.0" to "user@example.org",
            ) + additionalValues,
        )
    }

    private fun createAccount() = LegacyAccountDto(ACCOUNT_UUID).apply {
        incomingServerSettings = SERVER_SETTINGS
        outgoingServerSettings = SERVER_SETTINGS
        replaceIdentities(listOf(Identity(email = "user@example.org")))
    }

    private companion object {
        const val ACCOUNT_UUID = "00000000-0000-0000-0000-000000000000"

        val SERVER_SETTINGS = ServerSettings(
            type = "imap",
            host = "mail.example.org",
            port = 993,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "user@example.org",
            password = null,
            clientCertificateAlias = null,
        )

        val NO_OP_LOGGER = object : Logger {
            override fun verbose(tag: LogTag?, throwable: Throwable?, message: () -> LogMessage) = Unit
            override fun debug(tag: LogTag?, throwable: Throwable?, message: () -> LogMessage) = Unit
            override fun info(tag: LogTag?, throwable: Throwable?, message: () -> LogMessage) = Unit
            override fun warn(tag: LogTag?, throwable: Throwable?, message: () -> LogMessage) = Unit
            override fun error(tag: LogTag?, throwable: Throwable?, message: () -> LogMessage) = Unit
        }
    }
}
