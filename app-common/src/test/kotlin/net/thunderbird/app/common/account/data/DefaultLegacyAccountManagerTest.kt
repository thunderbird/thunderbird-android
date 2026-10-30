package net.thunderbird.app.common.account.data

import app.cash.turbine.test
import assertk.all
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.prop
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.android.account.AccountDefaultsProvider.Companion.UNASSIGNED_ACCOUNT_NUMBER
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto
import net.thunderbird.feature.account.storage.profile.ProfileDto
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultLegacyAccountManagerTest {

    private val accountStorage = FakeAccountLocalDataSource()
    private val accountCache = LegacyInMemoryAccountCache()
    private val accountDisplayOrderManager = FakeAccountDisplayOrderManager()
    private val accountFolderUpdater = FakeAccountFolderUpdater()
    private val testDispatcher = UnconfinedTestDispatcher()

    private val testSubject = DefaultLegacyAccountManager(
        accountStorage = accountStorage,
        accountCache = accountCache,
        accountDisplayOrderManager = accountDisplayOrderManager,
        accountFolderUpdater = accountFolderUpdater,
        backgroundDispatcher = testDispatcher,
    )

    @Test
    fun `findAll should return cached accounts after the initial load`() {
        val account1 = createFakeAccount("Account 1")
        accountStorage.accounts.add(account1)
        testSubject.findAll()

        val result = testSubject.findAll()

        assertThat(result).containsExactly(account1)
        assertThat(accountStorage.loadAllCalls).isEqualTo(1)
    }

    @Test
    fun `findAll should load from accountStorage and update cache when cache is empty`() {
        val account1 = createFakeAccount("Account 1")
        val account2 = createFakeAccount("Account 2")
        accountStorage.accounts.addAll(listOf(account1, account2))

        val result = testSubject.findAll()

        assertThat(result).containsExactly(account1, account2)
        assertThat(accountCache.findAll()).containsExactly(account1, account2)
        assertThat(accountStorage.loadAllCalls).isEqualTo(1)
    }

    @Test
    fun `findAll should load empty storage only once`() {
        assertThat(testSubject.findAll()).isEmpty()
        assertThat(testSubject.findAll()).isEmpty()

        assertThat(accountStorage.loadAllCalls).isEqualTo(1)
    }

    @Test
    fun `findById should not reload storage when an account is missing`() {
        val missingAccountId = AccountIdFactory.create()

        assertThat(testSubject.findById(missingAccountId)).isNull()
        assertThat(testSubject.findById(missingAccountId)).isNull()

        assertThat(accountStorage.loadAllCalls).isEqualTo(1)
    }

    @Test
    fun `findById should return cached account after the initial load`() {
        val account1 = createFakeAccount("Account 1")
        accountStorage.accounts.add(account1)
        testSubject.findAll()

        val result = testSubject.findById(account1.id)

        assertThat(result).isEqualTo(account1)
        assertThat(accountStorage.loadAllCalls).isEqualTo(1)
    }

    @Test
    fun `findById should return null when account is not found`() {
        val accountId = AccountIdFactory.create()

        val result = testSubject.findById(accountId)

        assertThat(result).isNull()
    }

    @Test
    fun `observeAll should load existing accounts before its first emission`() = runTest(testDispatcher) {
        // Arrange
        val finishedAccount = createFakeAccount("Finished Account").copy(isFinishedSetup = true)
        val unfinishedAccount = createFakeAccount("Unfinished Account")
        accountStorage.accounts.addAll(listOf(finishedAccount, unfinishedAccount))

        // Act and assert
        testSubject.observeAll().test {
            assertThat(awaitItem()).containsExactly(finishedAccount)
        }
        assertThat(accountStorage.loadAllCalls).isEqualTo(1)
    }

    @Test
    fun `observeById should load existing account before its first emission`() = runTest(testDispatcher) {
        // Arrange
        val account = createFakeAccount("Existing Account")
        accountStorage.accounts.add(account)

        // Act and assert
        testSubject.observeById(account.id).test {
            assertThat(awaitItem()).isEqualTo(account)
        }
        assertThat(accountStorage.loadAllCalls).isEqualTo(1)
    }

    @Test
    fun `observeAll should emit setup finished accounts reactively`() = runTest(testDispatcher) {
        val finishedAccount = createFakeAccount("Finished Account").copy(isFinishedSetup = true)

        testSubject.observeAll().test {
            assertThat(awaitItem()).isEmpty()

            accountCache.update(finishedAccount)

            assertThat(awaitItem()).containsExactly(finishedAccount)
        }
    }

    @Test
    fun `observeById should emit account by ID reactively`() = runTest(testDispatcher) {
        val account = createFakeAccount("Account 1")

        testSubject.observeById(account.id).test {
            assertThat(awaitItem()).isNull()

            accountCache.update(account)

            assertThat(awaitItem()).isEqualTo(account)
        }
    }

    @Test
    fun `createSync should assign account number if unassigned and save to storage and cache`() {
        val unassignedAccount = createFakeAccount("Unassigned").copy(accountNumber = UNASSIGNED_ACCOUNT_NUMBER)

        testSubject.createSync(unassignedAccount)

        val savedAccount = accountCache.findById(unassignedAccount.id)
        assertThat(savedAccount?.accountNumber).isEqualTo(0)
        assertThat(accountStorage.savedAccounts).containsExactly(savedAccount)
    }

    @Test
    fun `createSync should cache the account as stored instead of as passed in`() {
        // Arrange
        val account = createFakeAccount("Account").copy(openPgpProvider = null)
        // Storage writes a missing provider as an empty string, like the preferences storage
        accountStorage.onSave = { it.copy(openPgpProvider = "") }

        // Act
        testSubject.createSync(account)

        // Assert
        assertThat(testSubject.findById(account.id)).isNotNull().prop(LegacyAccount::openPgpProvider).isEqualTo("")
    }

    @Test
    fun `createSync should reject an account that already exists`() {
        // Arrange
        val account = createFakeAccount("Account")
        accountStorage.accounts.add(account)

        // Act and assert
        assertFailure { testSubject.createSync(account.copy(name = "Renamed")) }
            .isInstanceOf<IllegalArgumentException>()
        assertThat(accountStorage.savedAccounts).isEmpty()
    }

    @Test
    fun `updateSync with transform should return null and save nothing when the account does not exist`() {
        val result = testSubject.updateSync(AccountIdFactory.create()) { it.copy(name = "Renamed") }

        assertThat(result).isNull()
        assertThat(accountStorage.savedAccounts).isEmpty()
    }

    @Test
    fun `updateSync with transform should apply it to the latest account and return the saved account`() {
        // Arrange
        val account = createFakeAccount("Account")
        accountStorage.accounts.add(account)
        testSubject.updateSync(account.id) { it.copy(oAuthState = "refreshed") }

        // Act
        val result = testSubject.updateSync(account.id) { it.copy(name = "Renamed") }

        // Assert
        assertThat(result).isNotNull().all {
            prop(LegacyAccount::name).isEqualTo("Renamed")
            prop(LegacyAccount::oAuthState).isEqualTo("refreshed")
        }
        assertThat(testSubject.findById(account.id)).isEqualTo(result)
    }

    @Test
    fun `updateSync with transform should not lose concurrent updates`() {
        // Arrange
        val account = createFakeAccount("Account").copy(messagesNotificationChannelVersion = 0)
        accountStorage.accounts.add(account)
        val executor = Executors.newFixedThreadPool(CONCURRENT_THREADS)

        // Act
        try {
            val futures = List(CONCURRENT_THREADS) {
                executor.submit {
                    repeat(UPDATES_PER_THREAD) {
                        testSubject.updateSync(account.id) {
                            it.copy(messagesNotificationChannelVersion = it.messagesNotificationChannelVersion + 1)
                        }
                    }
                }
            }
            futures.forEach { it.get(OTHER_THREAD_TIMEOUT_SECONDS, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }

        // Assert
        assertThat(testSubject.findById(account.id)).isNotNull()
            .prop(LegacyAccount::messagesNotificationChannelVersion).isEqualTo(CONCURRENT_THREADS * UPDATES_PER_THREAD)
    }

    @Test
    fun `updateSync with transform should reject a change of the account ID`() {
        // Arrange
        val account = createFakeAccount("Account")
        accountStorage.accounts.add(account)

        // Act and assert
        assertFailure { testSubject.updateSync(account.id) { it.copy(id = AccountIdFactory.create()) } }
            .isInstanceOf<IllegalArgumentException>()
        assertThat(accountStorage.savedAccounts).isEmpty()
    }

    @Test
    fun `update with transform should save the account`() = runTest(testDispatcher) {
        // Arrange
        val account = createFakeAccount("Account")
        accountStorage.accounts.add(account)

        // Act
        testSubject.update(account.id) { it.copy(name = "Renamed") }

        // Assert
        assertThat(accountStorage.savedAccounts.single().name).isEqualTo("Renamed")
    }

    @Test
    fun `create should save assigned account to storage and cache`() = runTest(testDispatcher) {
        val account = createFakeAccount("Assigned").copy(accountNumber = 5)

        testSubject.create(account)

        assertThat(accountCache.findById(account.id)).isEqualTo(account)
        assertThat(accountStorage.savedAccounts).containsExactly(account)
    }

    @Test
    fun `updateSync should reset existing folder limits when display count changes`() {
        val account = createFakeAccount("Account").copy(displayCount = 25)
        accountStorage.accounts.add(account)

        testSubject.updateSync(account.id) { it.copy(displayCount = 50) }

        assertThat(accountFolderUpdater.resets).containsExactly(account.id to 50)
        assertThat(accountStorage.savedAccounts.single().displayCount).isEqualTo(50)
    }

    @Test
    fun `updateSync should not hold the account lock while resetting folder limits`() {
        // Arrange
        val account = createFakeAccount("Account").copy(displayCount = 25)
        accountStorage.accounts.add(account)
        val executor = Executors.newSingleThreadExecutor()
        var accountFoundFromOtherThread: LegacyAccount? = null
        // Simulates a message database migration on another thread that reads the account
        accountFolderUpdater.onReset = {
            accountFoundFromOtherThread = executor.submit<LegacyAccount?> { testSubject.findById(account.id) }
                .get(OTHER_THREAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        }

        try {
            // Act
            testSubject.updateSync(account.id) { it.copy(displayCount = 50) }
        } finally {
            executor.shutdownNow()
        }

        // Assert
        assertThat(accountFoundFromOtherThread).isNotNull().prop(LegacyAccount::displayCount).isEqualTo(50)
    }

    @Test
    fun `updateSync should not reset folder limits when display count is unchanged`() {
        val account = createFakeAccount("Account").copy(displayCount = 25)
        accountStorage.accounts.add(account)

        testSubject.updateSync(account.id) { it.copy(name = "Renamed") }

        assertThat(accountFolderUpdater.resets).isEmpty()
    }

    @Test
    fun `createSync should not reset folder limits for a new account`() {
        val account = createFakeAccount("New").copy(displayCount = 50)

        testSubject.createSync(account)

        assertThat(accountFolderUpdater.resets).isEmpty()
    }

    @Test
    fun `moveAccount should delegate to accountDisplayOrderManager and reload cache`() {
        val account1 = createFakeAccount("Account 1")
        val account2 = createFakeAccount("Account 2")
        accountStorage.accounts.addAll(listOf(account2, account1))

        testSubject.moveAccount(account1.id, 1)

        assertThat(accountDisplayOrderManager.movedAccountId).isEqualTo(account1.id)
        assertThat(accountDisplayOrderManager.movedNewPosition).isEqualTo(1)
        assertThat(accountCache.findAll()).containsExactly(account2, account1)
    }

    @Test
    fun `delete should remove account from storage and cache`() {
        val account1 = createFakeAccount("Account 1")
        accountStorage.save(account1)
        accountCache.update(account1)

        testSubject.delete(account1.id)

        assertThat(accountCache.findById(account1.id)).isNull()
        assertThat(accountStorage.deletedAccountIds).containsExactly(account1.id)
    }

    private companion object Companion {
        const val OTHER_THREAD_TIMEOUT_SECONDS = 5L
        const val CONCURRENT_THREADS = 4
        const val UPDATES_PER_THREAD = 50

        fun createFakeAccount(
            name: String,
            id: AccountId = AccountIdFactory.create(),
        ): LegacyAccount {
            return LegacyAccount(
                id = id,
                name = name,
                email = "user@example.com",
                profile = ProfileDto(
                    id = id,
                    name = name,
                    color = -1,
                    avatar = AvatarDto(
                        accountId = id,
                        avatarType = AvatarTypeDto.MONOGRAM,
                        avatarMonogram = "NA",
                        avatarImageUri = null,
                        avatarIconName = null,
                    ),
                ),
                incomingServerSettings = ServerSettings(
                    type = "imap",
                    host = "host",
                    port = 993,
                    connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
                    authenticationType = AuthType.PLAIN,
                    username = "user",
                    password = "pass",
                    clientCertificateAlias = null,
                ),
                outgoingServerSettings = ServerSettings(
                    type = "smtp",
                    host = "host",
                    port = 465,
                    connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
                    authenticationType = AuthType.PLAIN,
                    username = "user",
                    password = "pass",
                    clientCertificateAlias = null,
                ),
                identities = emptyList(),
            )
        }
    }

    private class FakeAccountFolderUpdater : AccountFolderUpdater {
        val resets = mutableListOf<Pair<AccountId, Int>>()
        var onReset: () -> Unit = {}

        override fun resetVisibleLimits(accountId: AccountId, visibleLimit: Int) {
            resets.add(accountId to visibleLimit)
            onReset()
        }
    }

    private class FakeAccountLocalDataSource : AccountLocalDataSource {
        val accounts = mutableListOf<LegacyAccount>()
        val savedAccounts = mutableListOf<LegacyAccount>()
        val deletedAccountIds = mutableListOf<AccountId>()
        var loadAllCalls = 0
            private set
        var onSave: (LegacyAccount) -> LegacyAccount = { it }

        override fun loadAll(): List<LegacyAccount> {
            loadAllCalls++
            return accounts.toList()
        }

        override fun getById(accountId: AccountId): LegacyAccount? {
            return accounts.find { it.id == accountId }
        }

        override fun save(account: LegacyAccount) {
            savedAccounts.add(account)
            accounts.removeAll { it.id == account.id }
            accounts.add(onSave(account))
        }

        override fun delete(accountId: AccountId) {
            deletedAccountIds.add(accountId)
            accounts.removeAll { it.id == accountId }
        }
    }

    private class FakeAccountDisplayOrderManager : AccountDisplayOrderManager {
        var movedAccountId: AccountId? = null
        var movedNewPosition: Int? = null

        override fun moveToPosition(accountId: AccountId, newPosition: Int) {
            movedAccountId = accountId
            movedNewPosition = newPosition
        }
    }
}
