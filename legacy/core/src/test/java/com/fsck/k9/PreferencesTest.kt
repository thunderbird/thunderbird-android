package com.fsck.k9

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isSameInstanceAs
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import kotlin.test.Test
import net.thunderbird.account.fake.FakeAccountData.ACCOUNT_ID
import net.thunderbird.account.fake.FakeAccountData.ACCOUNT_ID_OTHER
import net.thunderbird.account.fake.FakeAccountData.ACCOUNT_ID_OTHER_RAW
import net.thunderbird.account.fake.FakeAccountData.ACCOUNT_ID_RAW
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.android.preferences.TestStoragePersister
import net.thunderbird.components.core.logging.Logger
import net.thunderbird.components.core.logging.testing.TestLogger
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.storage.legacy.LegacyAccountStorageHandler
import net.thunderbird.feature.account.storage.legacy.LegacyAvatarDtoStorageHandler
import net.thunderbird.feature.account.storage.legacy.LegacyProfileDtoStorageHandler
import org.junit.Before
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class PreferencesTest {

    private val logger: Logger = TestLogger()

    private val preferences = Preferences(
        storagePersister = TestStoragePersister(
            logger = logger,
        ),
        localStoreProvider = mock(),
        legacyAccountStorageHandler = LegacyAccountStorageHandler(
            serverSettingsDtoSerializer = mock {
                on { serialize(any()) } doReturn ""
                on { deserialize(any()) } doReturn SERVER_SETTINGS
            },
            profileDtoStorageHandler = LegacyProfileDtoStorageHandler(
                avatarDtoStorageHandler = LegacyAvatarDtoStorageHandler(),
            ),
            logger,
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
        createAndSaveAccount(ACCOUNT_ID)
        createAndSaveAccount(ACCOUNT_ID_OTHER)
        val firstAccountOne = preferences.getById(ACCOUNT_ID)

        preferences.loadAccounts()

        val firstAccountTwo = preferences.getById(ACCOUNT_ID)
        assertThat(firstAccountTwo).isSameInstanceAs(firstAccountOne)
    }

    @Test
    fun `saving accounts should return updated Account instance`() {
        val account = createAccount(ACCOUNT_ID)
        preferences.saveAccount(account)

        val updatedAccount = createAccount(ACCOUNT_ID).apply {
            name = "New name"
        }

        preferences.saveAccount(updatedAccount)

        val currentAccountOne = preferences.getById(ACCOUNT_ID)!!
        assertThat(currentAccountOne.name).isEqualTo("New name")
    }

    private fun createAccount(accountId: AccountId): LegacyAccountDto {
        return LegacyAccountDto(
            id = accountId,
            isSensitiveDebugLoggingEnabled = { false },
        ).apply {
            incomingServerSettings = SERVER_SETTINGS
            outgoingServerSettings = SERVER_SETTINGS
        }
    }

    private fun createAndSaveAccount(accountId: AccountId) {
        val account = preferences.newAccount(accountId).apply {
            // To be able to persist `Account` we need to set server settings
            incomingServerSettings = SERVER_SETTINGS
            outgoingServerSettings = SERVER_SETTINGS
        }

        preferences.saveAccount(account)
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
