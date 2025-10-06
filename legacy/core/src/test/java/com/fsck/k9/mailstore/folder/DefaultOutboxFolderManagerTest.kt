package com.fsck.k9.mailstore.folder

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mailstore.LocalStore
import com.fsck.k9.mailstore.LocalStoreProvider
import com.fsck.k9.mailstore.LockableDatabase
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.architecture.model.Id
import net.thunderbird.core.common.cache.TimeLimitedCache
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.Account
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.mail.account.api.AccountManager
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
class DefaultOutboxFolderManagerTest {
    private val logger = TestLogger()

    @Test
    fun `getOutboxFolderId should return cached value when available`() = runTest {
        // Arrange
        val (accountId, account) = createAccountPair()
        val accountManager = FakeLegacyAccountManager(initialAccounts = listOf(account))
        val localStoreProvider = createLocalStoreProvider(account)
        val expectedFolderId = 123L
        val cache = TimeLimitedCache<AccountId, Long>()
        cache.set(accountId, expectedFolderId)
        val subject = DefaultOutboxFolderManager(
            logger = logger,
            accountManager = accountManager,
            localStoreProvider = localStoreProvider,
            outboxFolderIdCache = cache,
            ioDispatcher = Dispatchers.Unconfined,
        )

        // Act
        val result = subject.getOutboxFolderId(accountId, createIfMissing = true)

        // Assert
        assertThat(result).isEqualTo(expectedFolderId)
    }

    @Test
    fun `getOutboxFolderId should read from DB when not cached and folder exists`() = runTest {
        // Arrange
        val (accountId, account) = createAccountPair()
        val accountManager = FakeLegacyAccountManager(initialAccounts = listOf(account))
        val expectedId = 1L
        val localStoreProvider = createLocalStoreProvider(account = account, folderId = expectedId)
        val cache = TimeLimitedCache<AccountId, Long>()
        val subject = DefaultOutboxFolderManager(
            logger = logger,
            accountManager = accountManager,
            localStoreProvider = localStoreProvider,
            outboxFolderIdCache = cache,
            ioDispatcher = Dispatchers.Unconfined,
        )

        // Act
        val result = subject.getOutboxFolderId(accountId, createIfMissing = true)

        // Assert
        assertThat(result).isEqualTo(expectedId)
    }

    @Test
    fun `getOutboxFolderId should read from DB and refill cache when cached value expired`() = runTest {
        // Arrange
        val (accountId, account) = createAccountPair()
        val accountManager = FakeLegacyAccountManager(initialAccounts = listOf(account))

        val expectedFolderId = 42L
        val localStoreProvider = createLocalStoreProvider(account = account, folderId = expectedFolderId)
        val fakeClock = FakeClock(nowInstant = Clock.System.now())
        val cache = TimeLimitedCache<AccountId, Long>(clock = fakeClock)

        // Put a value into the cache and then advance time so it expires
        cache.set(accountId, 999L, expiresIn = 1.hours)
        fakeClock.advanceBy(2.hours)

        val subject = DefaultOutboxFolderManager(
            logger = logger,
            accountManager = accountManager,
            localStoreProvider = localStoreProvider,
            outboxFolderIdCache = cache,
            ioDispatcher = Dispatchers.Unconfined,
        )

        // Act
        val result = subject.getOutboxFolderId(accountId, createIfMissing = false)

        // Assert: result is read from DB and cache is repopulated
        assertThat(result).isEqualTo(expectedFolderId)
        assertThat(cache.getValue(accountId)).isEqualTo(expectedFolderId)
    }

    @Test
    fun `getOutboxFolderId should create folder when not found and createIfMissing true`() = runTest {
        // Arrange
        val (accountId, account) = createAccountPair()
        val accountManager = FakeLegacyAccountManager(initialAccounts = listOf(account))
        val cursor = mock<Cursor> {
            on { moveToFirst() } doReturn false
        }
        val expectedFolderId = 42L
        val localStoreProvider = createLocalStoreProvider(
            account = account,
            folderId = expectedFolderId,
            moveToFirst = false,
        )
        val cache = TimeLimitedCache<AccountId, Long>()
        val subject = DefaultOutboxFolderManager(
            logger = logger,
            accountManager = accountManager,
            localStoreProvider = localStoreProvider,
            outboxFolderIdCache = cache,
            ioDispatcher = Dispatchers.Unconfined,
        )

        // Act
        val result = subject.getOutboxFolderId(accountId, createIfMissing = true)

        // Assert
        assertThat(result).isEqualTo(expectedFolderId)
    }

    @Test
    fun `createOutboxFolder should return Success when LocalStore creates folder`() = runTest {
        // Arrange
        val (accountId, account) = createAccountPair()
        val accountManager = FakeLegacyAccountManager(initialAccounts = listOf(account))
        val expectedFolderId = 99L
        val localStore = mock<LocalStore> {
            on { createLocalFolder(any(), any(), any(), any()) } doReturn expectedFolderId
        }
        val localStoreProvider = mock<LocalStoreProvider> {
            on { getInstance(account) } doReturn localStore
        }
        val cache = TimeLimitedCache<AccountId, Long>()
        val subject = DefaultOutboxFolderManager(
            logger = logger,
            accountManager = accountManager,
            localStoreProvider = localStoreProvider,
            outboxFolderIdCache = cache,
            ioDispatcher = Dispatchers.Unconfined,
        )

        // Act
        val outcome = subject.createOutboxFolder(accountId)

        // Assert
        assertThat(outcome.isSuccess).isTrue()
        val data = (outcome as Outcome.Success).data
        assertThat(data).isEqualTo(expectedFolderId)
    }

    @Test
    fun `createOutboxFolder should return Failure when LocalStore throws`() = runTest {
        // Arrange
        val (accountId, account) = createAccountPair()
        val accountManager = FakeLegacyAccountManager(initialAccounts = listOf(account))
        val localStore = mock<LocalStore> {
            on { createLocalFolder(any(), any(), any(), any()) } doAnswer { throw MessagingException("boom") }
        }
        val localStoreProvider = mock<LocalStoreProvider> {
            on { getInstance(account) } doReturn localStore
        }
        val cache = TimeLimitedCache<AccountId, Long>()
        val subject = DefaultOutboxFolderManager(
            logger = logger,
            accountManager = accountManager,
            localStoreProvider = localStoreProvider,
            outboxFolderIdCache = cache,
            ioDispatcher = Dispatchers.Unconfined,
        )

        // Act
        val outcome = subject.createOutboxFolder(accountId)

        // Assert
        assertThat(outcome.isFailure).isTrue()
    }

    @Test
    fun `hasPendingMessages should return true when DB count is greater than zero`() = runTest {
        // Arrange
        val (accountId, account) = createAccountPair()
        val accountManager = FakeLegacyAccountManager(initialAccounts = listOf(account))
        val expectedCount = 123
        val localStoreProvider = createLocalStoreProvider(account = account, count = expectedCount)
        val cache = TimeLimitedCache<AccountId, Long>()
        val subject = DefaultOutboxFolderManager(
            logger = logger,
            accountManager = accountManager,
            localStoreProvider = localStoreProvider,
            outboxFolderIdCache = cache,
            ioDispatcher = Dispatchers.Unconfined,
        )

        // Act
        val result = subject.hasPendingMessages(accountId)

        // Assert
        assertThat(result).isTrue()
    }

    @Test
    fun `hasPendingMessages should return false when DB count is zero`() = runTest {
        // Arrange
        val (accountId, account) = createAccountPair()
        val accountManager = FakeLegacyAccountManager(initialAccounts = listOf(account))
        val expectedCount = 0
        val localStoreProvider = createLocalStoreProvider(account = account, count = expectedCount)
        val cache = TimeLimitedCache<AccountId, Long>()
        val subject = DefaultOutboxFolderManager(
            logger = logger,
            accountManager = accountManager,
            localStoreProvider = localStoreProvider,
            outboxFolderIdCache = cache,
            ioDispatcher = Dispatchers.Unconfined,
        )

        // Act
        val result = subject.hasPendingMessages(accountId)

        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun `hasPendingMessages should return false when DB throws MessagingException`() = runTest {
        // Arrange
        val (accountId, account) = createAccountPair()
        val accountManager = FakeLegacyAccountManager(initialAccounts = listOf(account))
        val localStoreProvider = createLocalStoreProvider(
            account = account,
            messagingException = MessagingException("db-fail"),
        )
        val cache = TimeLimitedCache<AccountId, Long>()
        val subject = DefaultOutboxFolderManager(
            logger = logger,
            accountManager = accountManager,
            localStoreProvider = localStoreProvider,
            outboxFolderIdCache = cache,
            ioDispatcher = Dispatchers.Unconfined,
        )

        // Act
        val result = subject.hasPendingMessages(accountId)

        // Assert
        assertThat(result).isFalse()
    }

    private fun createAccountPair(): Pair<Id<Account>, LegacyAccount> {
        val accountId = AccountIdFactory.of(Uuid.random().toString())
        val incoming = ServerSettings(
            type = "imap",
            host = "example.com",
            port = 993,
            connectionSecurity = ConnectionSecurity.NONE,
            authenticationType = AuthType.PLAIN,
            username = "user",
            password = "pass",
            clientCertificateAlias = null,
        )
        val outgoing = ServerSettings(
            type = "smtp",
            host = "example.com",
            port = 587,
            connectionSecurity = ConnectionSecurity.NONE,
            authenticationType = AuthType.PLAIN,
            username = "user",
            password = "pass",
            clientCertificateAlias = null,
        )
        return accountId to LegacyAccount(
            uuid = accountId.asRaw(),
        ).apply {
            name = "acc"
            identities = listOf(Identity(name = "n", email = "user@example.com")).toMutableList()
            incomingServerSettings = incoming
            outgoingServerSettings = outgoing
        }
    }

    private fun createLocalStoreProvider(
        account: LegacyAccount,
        folderId: Long? = 1L,
        count: Int? = null,
        moveToFirst: Boolean = folderId != null || count != null,
        messagingException: MessagingException? = null,
    ): LocalStoreProvider {
        val cursor = mock<Cursor> {
            on { moveToFirst() } doReturn moveToFirst
            folderId?.let { on { getLong(0) } doReturn it }
            count?.let { on { getInt(0) } doReturn it }
        }
        val db = mock<SQLiteDatabase> {
            if (messagingException == null) {
                on { rawQuery(any(), any()) } doReturn cursor
            } else {
                on { rawQuery(any(), any()) } doAnswer { throw messagingException }
            }
        }
        val lockableDb = mock<LockableDatabase> {
            on { execute(any(), any<LockableDatabase.DbCallback<Any>>()) } doAnswer { invocation ->
                val callback = invocation.getArgument<LockableDatabase.DbCallback<Any>>(1)
                callback.doDbWork(db)
            }
        }
        val localStore = mock<LocalStore> {
            on { database } doReturn lockableDb
            folderId?.let { folderId ->
                on {
                    createLocalFolder(any(), any(), any(), any())
                } doReturn folderId
            }
        }
        val localStoreProvider = mock<LocalStoreProvider> {
            on { getInstance(account) } doReturn localStore
        }
        return localStoreProvider
    }
}

private class FakeLegacyAccountManager(
    initialAccounts: List<LegacyAccount> = emptyList(),
) : AccountManager<LegacyAccount> {
    private val accountsState = MutableStateFlow(initialAccounts)

    override fun getAccounts(): List<LegacyAccount> = accountsState.value

    override fun getAccountsFlow(): Flow<List<LegacyAccount>> = accountsState

    override fun getAccount(accountUuid: String): LegacyAccount? =
        accountsState.value.find { it.uuid == accountUuid }

    override fun getAccountFlow(accountUuid: String): Flow<LegacyAccount?> =
        accountsState.map { list -> list.find { it.uuid == accountUuid } }

    override fun moveAccount(account: LegacyAccount, newPosition: Int) {
        // no-op for tests
    }

    override fun saveAccount(account: LegacyAccount) {
        // no-op for tests
    }
}

@OptIn(ExperimentalTime::class)
private class FakeClock(var nowInstant: Instant) : Clock {
    override fun now(): Instant = nowInstant
    fun advanceBy(duration: Duration) {
        nowInstant += duration
    }
}
