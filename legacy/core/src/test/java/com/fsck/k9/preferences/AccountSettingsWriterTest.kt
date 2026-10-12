package com.fsck.k9.preferences

import app.k9mail.legacy.di.DI.get
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import com.fsck.k9.K9RobolectricTest
import com.fsck.k9.account.FakeStorageBackedAccountManager
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.android.account.AccountDefaultsProvider
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.preference.storage.StorageEditor
import net.thunderbird.core.preference.storage.StorageProvider
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.storage.legacy.AccountStorageHandler
import org.junit.Before
import org.junit.Test
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class AccountSettingsWriterTest : K9RobolectricTest() {
    private val accountManagerModule = module {
        single<LegacyAccountManager> {
            CachingAccountManager(
                FakeStorageBackedAccountManager(
                    storageProvider = get<StorageProvider>(),
                    accountStorageHandler = get<AccountStorageHandler>(),
                    accountDefaultsProvider = get<AccountDefaultsProvider>(),
                ),
            )
        }
    }

    @Before
    fun setUp() {
        loadKoinModules(accountManagerModule)
    }

    @Test
    fun `write makes imported account visible to an already loaded manager`() = runTest {
        // Arrange
        val accountManager = get<LegacyAccountManager>()
        assertThat(accountManager.findAll().size).isEqualTo(0)
        val accountId = AccountIdFactory.of(UUID.randomUUID().toString())
        val testSubject = get<AccountSettingsWriter>()
        val account = createValidatedAccount(accountId)

        // Act
        val result = testSubject.write(account)

        // Assert
        assertThat(result.second.uuid).isEqualTo(accountId.toString())
        val cachedAccount = accountManager.findById(accountId)
        assertThat(cachedAccount).isNotNull()
        assertThat(cachedAccount?.profile?.name).isEqualTo("Imported")
        assertThat(accountManager.observeAll().first().single().id).isEqualTo(accountId)
        assertThat(get<StorageProvider>().loadLatestStorage().getStringOrNull("accountUuids").orEmpty())
            .contains(accountId.toString())
    }

    @Test
    fun `failed folder settings commit does not save an account`() = runTest {
        // Arrange
        val accountManager = get<LegacyAccountManager>()
        assertThat(accountManager.findAll()).isEmpty()
        val accountId = AccountIdFactory.of(UUID.randomUUID().toString())
        val failingEditor = mock<StorageEditor> {
            on { commit() } doReturn false
        }
        val storageProvider = mock<StorageProvider> {
            on { createStorageEditor() } doReturn failingEditor
        }
        val testSubject = AccountSettingsWriter(
            accountManager = accountManager,
            storageProvider = storageProvider,
            localFoldersCreator = get(),
            clock = get(),
            generalSettingsManager = get(),
            serverSettingsDtoSerializer = get(),
            context = get(),
        )
        val account = createValidatedAccount(accountId).copy(
            folders = listOf(ValidatedSettings.Folder("INBOX", emptyMap())),
        )

        // Act
        assertFailure { testSubject.write(account) }.isInstanceOf<IllegalStateException>()

        // Assert
        assertThat(accountManager.findAll()).isEmpty()
        assertThat(accountManager.findById(accountId)).isEqualTo(null)
        assertThat(accountManager.observeAll().first()).isEmpty()
        assertThat(get<StorageProvider>().loadLatestStorage().getStringOrNull("accountUuids")).isEqualTo(null)
    }

    private fun createValidatedAccount(accountId: AccountId): ValidatedSettings.Account {
        val serverSettings = mapOf<String, Any?>(
            "host" to "example.com",
            "port" to 993,
            "connectionSecurity" to "SSL_TLS_REQUIRED",
            "authenticationType" to "PLAIN",
            "username" to "user",
            "password" to null,
            "clientCertificateAlias" to null,
        )
        return ValidatedSettings.Account(
            uuid = accountId.toString(),
            name = "Imported",
            incoming = ValidatedSettings.Server("imap", serverSettings, emptyMap()),
            outgoing = ValidatedSettings.Server("smtp", serverSettings, emptyMap()),
            settings = AccountSettingsDescriptions.validate(Settings.VERSION, emptyMap(), true),
            identities = listOf(
                ValidatedSettings.Identity(
                    name = "Sender",
                    email = "user@example.com",
                    description = null,
                    settings = IdentitySettingsDescriptions.validate(Settings.VERSION, emptyMap(), true),
                ),
            ),
            folders = emptyList(),
        )
    }
}

private class CachingAccountManager(private val storageManager: LegacyAccountManager) : LegacyAccountManager {
    private val accounts = MutableStateFlow(storageManager.findAll())

    override fun findAll(): List<LegacyAccount> = accounts.value

    override fun observeAll(): Flow<List<LegacyAccount>> = accounts

    override fun findById(accountId: AccountId): LegacyAccount? = findAll().firstOrNull { it.id == accountId }

    override fun observeById(accountId: AccountId): Flow<LegacyAccount?> =
        accounts.map { list -> list.firstOrNull { it.id == accountId } }

    override fun moveAccount(accountId: AccountId, newPosition: Int) = error("Not supported")

    override suspend fun update(account: LegacyAccount) = updateSync(account)

    override fun updateSync(account: LegacyAccount) {
        storageManager.updateSync(account)
        accounts.value = storageManager.findAll()
    }
}
