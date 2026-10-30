package com.fsck.k9.preferences

import app.k9mail.legacy.di.DI.get
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.first
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.prop
import com.fsck.k9.K9RobolectricTest
import com.fsck.k9.account.FakeStorageBackedAccountManager
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.android.account.AccountDefaultsProvider
import net.thunderbird.core.android.account.AccountDefaultsProvider.Companion.UNASSIGNED_ACCOUNT_NUMBER
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.core.preference.storage.StorageEditor
import net.thunderbird.core.preference.storage.StorageProvider
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.storage.legacy.AccountStorageHandler
import org.junit.Before
import org.junit.Test
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

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
        assertThat(get<StorageProvider>().storage.getStringOrNull("accountUuids").orEmpty())
            .contains(accountId.toString())
    }

    @Test
    fun `write leaves the account number to the account manager`() = runTest {
        // Arrange
        val accountManager = get<LegacyAccountManager>() as CachingAccountManager
        val testSubject = get<AccountSettingsWriter>()
        val account = createValidatedAccount(AccountIdFactory.create())

        // Act
        testSubject.write(account)

        // Assert
        assertThat(accountManager.updatedAccounts).first()
            .prop(LegacyAccount::accountNumber).isEqualTo(UNASSIGNED_ACCOUNT_NUMBER)
    }

    @Test
    fun `failed folder settings commit does not save an account`() = runTest {
        // Arrange
        val accountManager = get<LegacyAccountManager>()
        assertThat(accountManager.findAll()).isEmpty()
        val accountId = AccountIdFactory.of(UUID.randomUUID().toString())
        val testSubject = AccountSettingsWriter(
            accountManager = accountManager,
            storageProvider = FailingCommitStorageProvider(get<StorageProvider>()),
            localFoldersCreator = get(),
            clock = get(),
            generalSettingsManager = get(),
            serverSettingsDtoSerializer = get(),
            avatarImageRepository = get(),
            context = get(),
        )
        val account = createValidatedAccount(accountId).copy(
            folders = listOf(ValidatedSettings.Folder("INBOX", emptyMap())),
        )

        // Act
        assertFailure { testSubject.write(account) }.isInstanceOf<IllegalStateException>()

        // Assert
        assertThat(accountManager.findAll()).isEmpty()
        assertThat(accountManager.findById(accountId)).isNull()
        assertThat(get<StorageProvider>().storage.getStringOrNull("accountUuids")).isNull()
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

/** Reads from [delegate], but every commit fails without writing anything. */
private class FailingCommitStorageProvider(private val delegate: StorageProvider) : StorageProvider {
    override val storage: Storage get() = delegate.storage

    override fun createStorageEditor(): StorageEditor = FailingCommitStorageEditor()
}

private class FailingCommitStorageEditor : StorageEditor {
    override fun putBoolean(key: String, value: Boolean): StorageEditor = this
    override fun putInt(key: String, value: Int): StorageEditor = this
    override fun putLong(key: String, value: Long): StorageEditor = this
    override fun putString(key: String, value: String?): StorageEditor = this
    override fun remove(key: String): StorageEditor = this
    override fun commit(): Boolean = false
}

private class CachingAccountManager(private val storageManager: LegacyAccountManager) : LegacyAccountManager {
    private val accounts = MutableStateFlow(storageManager.findAll())

    val updatedAccounts = mutableListOf<LegacyAccount>()

    override fun findAll(): List<LegacyAccount> = accounts.value

    override fun observeAll(): Flow<List<LegacyAccount>> = accounts

    override fun findById(accountId: AccountId): LegacyAccount? = findAll().firstOrNull { it.id == accountId }

    override fun observeById(accountId: AccountId): Flow<LegacyAccount?> =
        accounts.map { list -> list.firstOrNull { it.id == accountId } }

    override fun moveAccount(accountId: AccountId, newPosition: Int) = error("Not supported")

    override suspend fun create(account: LegacyAccount) = createSync(account)

    override fun createSync(account: LegacyAccount) {
        updatedAccounts.add(account)
        storageManager.createSync(account)
        accounts.value = storageManager.findAll()
    }

    override suspend fun update(accountId: AccountId, transform: (LegacyAccount) -> LegacyAccount): LegacyAccount? =
        updateSync(accountId, transform)

    override fun updateSync(accountId: AccountId, transform: (LegacyAccount) -> LegacyAccount): LegacyAccount? =
        findById(accountId)?.let(transform)?.also { createSync(it) }
}
