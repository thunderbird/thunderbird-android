package com.fsck.k9.mailstore

import app.k9mail.legacy.mailstore.FolderSettings
import app.k9mail.legacy.mailstore.MessageStoreManager
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.K9RobolectricTest
import com.fsck.k9.Preferences
import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import net.thunderbird.core.android.account.LegacyAccount
import org.junit.After
import org.junit.Test
import org.koin.core.component.inject
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class K9BackendDefaultStorageTest : K9RobolectricTest() {
    val preferences: Preferences by inject()
    val messageStoreManager: MessageStoreManager by inject()
    val saveMessageDataCreator: SaveMessageDataCreator by inject()

    val account: LegacyAccount = createAccount()
    val backendStorage = createBackendStorage()

    @After
    fun tearDown() {
        preferences.deleteAccount(account)
    }

    @Test
    fun writeAndReadExtraString() {
        backendStorage.setExtraString("testString", "someValue")
        val value = backendStorage.getExtraString("testString")

        assertThat(value).isEqualTo("someValue")
    }

    @Test
    fun updateExtraString() {
        backendStorage.setExtraString("testString", "oldValue")
        backendStorage.setExtraString("testString", "newValue")

        val value = backendStorage.getExtraString("testString")
        assertThat(value).isEqualTo("newValue")
    }

    @Test
    fun writeAndReadExtraInteger() {
        backendStorage.setExtraNumber("testNumber", 42)
        val value = backendStorage.getExtraNumber("testNumber")

        assertThat(value).isEqualTo(42L)
    }

    @Test
    fun updateExtraInteger() {
        backendStorage.setExtraNumber("testNumber", 42)
        backendStorage.setExtraNumber("testNumber", 23)

        val value = backendStorage.getExtraNumber("testNumber")
        assertThat(value).isEqualTo(23L)
    }

    @Suppress("ForbiddenComment")
    fun createAccount(): LegacyAccount {
        // FIXME: This is a hack to get Preferences into a state where it's safe to call newAccount()
        preferences.clearAccounts()

        return preferences.newAccount().apply {
            incomingServerSettings = SERVER_SETTINGS
            outgoingServerSettings = SERVER_SETTINGS
        }
    }

    private fun createBackendStorage(): BackendStorage {
        val messageStore = messageStoreManager.getMessageStore(account)
        val folderSettingsProvider = createFolderSettingsProvider()
        return K9BackendStorage(messageStore, folderSettingsProvider, saveMessageDataCreator, emptyList())
    }

    companion object {
        private val SERVER_SETTINGS = ServerSettings(
            type = "irrelevant",
            host = "irrelevant",
            port = 993,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "username",
            password = null,
            clientCertificateAlias = null,
        )
    }
}

internal fun createFolderSettingsProvider(): FolderSettingsProvider {
    return mock {
        on { getFolderSettings(any()) } doReturn
            FolderSettings(
                visibleLimit = 25,
                isVisible = true,
                isSyncEnabled = false,
                isNotificationsEnabled = false,
                isPushEnabled = false,
                inTopGroup = false,
                integrate = false,
            )
    }
}
