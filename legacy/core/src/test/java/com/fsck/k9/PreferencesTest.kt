package com.fsck.k9

import assertk.assertThat
import assertk.assertions.isSameInstanceAs
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.preferences.InMemoryStoragePersister
import kotlin.test.Test
import org.junit.Before
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class PreferencesTest {
    private val preferences = Preferences(
        storagePersister = InMemoryStoragePersister(),
        localStoreProvider = mock(),
        accountPreferenceSerializer = AccountPreferenceSerializer(
            resourceProvider = mock(),
            serverSettingsSerializer = mock {
                on { serialize(any()) } doReturn ""
                on { deserialize(any()) } doReturn SERVER_SETTINGS
            },
        ),
        accountDefaultsProvider = mock(),
    )

    @Before
    fun setUp() {
        // Currently necessary for initialization
        preferences.loadAccounts()
    }

    @Test
    fun `reloading accounts should return same Account instance`() {
        createAccount(ACCOUNT_UUID_ONE)
        createAccount(ACCOUNT_UUID_TWO)
        val firstAccountOne = preferences.getAccount(ACCOUNT_UUID_ONE)

        preferences.loadAccounts()

        val firstAccountTwo = preferences.getAccount(ACCOUNT_UUID_ONE)
        assertThat(firstAccountTwo).isSameInstanceAs(firstAccountOne)
    }

    private fun createAccount(accountUuid: String) {
        val account = preferences.newAccount(accountUuid).apply {
            // To be able to persist `Account` we need to set server settings
            incomingServerSettings = SERVER_SETTINGS
            outgoingServerSettings = SERVER_SETTINGS
        }

        preferences.saveAccount(account)
    }

    companion object {
        private const val ACCOUNT_UUID_ONE = "account-one"
        private const val ACCOUNT_UUID_TWO = "account-two"

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
