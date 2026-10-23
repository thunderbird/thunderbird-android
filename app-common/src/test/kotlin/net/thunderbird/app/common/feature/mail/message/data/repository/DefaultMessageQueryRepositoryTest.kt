package net.thunderbird.app.common.feature.mail.message.data.repository

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import app.k9mail.legacy.mailstore.ListenableMessageStore
import app.k9mail.legacy.mailstore.MessageStore
import app.k9mail.legacy.mailstore.MessageStoreFactory
import app.k9mail.legacy.mailstore.MessageStoreManager
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.mailstore.LocalStore
import com.fsck.k9.mailstore.LocalStoreProvider
import com.fsck.k9.mailstore.LockableDatabase
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.thunderbird.app.common.feature.mail.FakeData
import net.thunderbird.components.core.logging.testing.TestLogger
import net.thunderbird.components.core.outcome.Outcome
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.android.account.fake.FakeLegacyAccountManager
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.mail.folder.FolderIdFactory
import net.thunderbird.feature.mail.folder.LegacyFolderIdFactory
import net.thunderbird.feature.mail.message.LegacyMessageIdFactory
import net.thunderbird.feature.mail.message.MessageServerId
import net.thunderbird.feature.mail.message.domain.GetMessageIdCriteria
import net.thunderbird.feature.mail.message.domain.MessageQueryError
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

private const val FOLDER_ID = 7L
private const val SERVER_ID = "100"
private const val UNKNOWN_ACCOUNT_ID = "22222222-2222-2222-2222-222222222222"

/**
 * [MessageStore] is mocked here: it is a 60+ method legacy interface and only the calls the repository makes are
 * stubbed. [LegacyAccountManager] is a mock that only answers the account lookup. [LocalStoreProvider] is not
 * touched by the queries under test and is a plain mock.
 */
@Suppress("MaxLineLength")
class DefaultMessageQueryRepositoryTest {
    private val account: LegacyAccount = FakeData.legacyAccount
    private val messageStore = mock<MessageStore>()
    private val accountManager = mock<LegacyAccountManager>()
    private val messageStoreManager = MessageStoreManager(
        messageStoreFactory = QueryTestMessageStoreFactory(
            messageStoresByUuid = mapOf(
                account.id to ListenableMessageStore(messageStore, mainImmediateDispatcher = Dispatchers.Unconfined),
            ),
        ),
    )

    private val accountId = AccountIdFactory.create()
    private val folderId = FolderIdFactory.of("00000000-0000-4000-8000-000000000010")
    private val legacyFolderId = LegacyFolderIdFactory.toLegacyId(folderId)
    private val serverId = "server-msg-id-123"
    private val criteria = GetMessageIdCriteria(
        folderId = folderId,
        messageServerId = MessageServerId(serverId),
    )

    private val cursor = mock<Cursor>()
    private val sqliteDatabase = mock<SQLiteDatabase> {
        on {
            query(
                eq("messages"),
                eq(arrayOf("id")),
                eq("folder_id = ? AND uid = ?"),
                eq(arrayOf(legacyFolderId.toString(), serverId)),
                eq(null),
                eq(null),
                eq(null),
            )
        } doReturn cursor
    }

    private val lockableDatabase = mock<LockableDatabase> {
        on { execute(eq(false), any<LockableDatabase.DbCallback<Any?>>()) } doAnswer { invocation ->
            val callback = invocation.getArgument<LockableDatabase.DbCallback<Any?>>(1)
            callback.doDbWork(sqliteDatabase)
        }
    }

    private val localStore = mock<LocalStore> {
        on { database } doReturn lockableDatabase
    }

    private val localStoreProvider = mock<LocalStoreProvider> {
        on { getInstance(accountId) } doReturn localStore
    }

    private val logger = TestLogger()
    private val testSubject = DefaultMessageQueryRepository(
        logger = logger,
        accountManager = FakeLegacyAccountManager(listOf(account)),
        localStoreProvider = localStoreProvider,
        messageStoreManager = messageStoreManager,
        ioDispatcher = UnconfinedTestDispatcher(),
    )

    // region [findIdByCriteria]

    @Test
    fun `findIdByCriteria returns message id when matching message is found`() = runTest {
        val expectedLegacyMessageId = 42L
        whenever(cursor.moveToFirst()).thenReturn(true)
        whenever(cursor.getLong(0)).thenReturn(expectedLegacyMessageId)

        val result = testSubject.findIdByCriteria(accountId, criteria)

        assertThat(result).isEqualTo(Outcome.success(LegacyMessageIdFactory.of(expectedLegacyMessageId)))
        verify(localStoreProvider).getInstance(accountId)
    }

    @Test
    fun `findIdByCriteria returns null when no matching message is found`() = runTest {
        whenever(cursor.moveToFirst()).thenReturn(false)

        val result = testSubject.findIdByCriteria(accountId, criteria)

        assertThat(result).isEqualTo(Outcome.success(null))
        verify(localStoreProvider).getInstance(accountId)
    }

    // endregion [findIdByCriteria]

    // region [getAllServerIdByFolderId]

    @Test
    fun `getAllServerIdByFolderId should return the server ids reported by the store for the folder`() = runTest {
        // Arrange
        stubKnownAccount()
        whenever(messageStore.getMessageServerIds(FOLDER_ID)).thenReturn(setOf("100", "101", "102"))

        // Act
        val outcome = testSubject.getAllServerIdByFolderId(folderId, accountId)

        // Assert
        assertThat(outcome).isEqualTo(
            Outcome.success(setOf(MessageServerId("100"), MessageServerId("101"), MessageServerId("102"))),
        )
        verify(messageStore).getMessageServerIds(FOLDER_ID)
        verifyNoInteractions(localStoreProvider)
    }

    @Test
    fun `getAllServerIdByFolderId should return an empty set when the folder has no messages`() = runTest {
        // Arrange
        stubKnownAccount()
        whenever(messageStore.getMessageServerIds(FOLDER_ID)).thenReturn(emptySet())

        // Act
        val outcome = testSubject.getAllServerIdByFolderId(folderId, accountId)

        // Assert
        assertThat(outcome).isEqualTo(Outcome.success(emptySet<MessageServerId>()))
    }

    @Test
    fun `getAllServerIdByFolderId should return UnhandledError when the store throws`() = runTest {
        // Arrange
        stubKnownAccount()
        val exception = IllegalStateException("database is locked")
        whenever(messageStore.getMessageServerIds(FOLDER_ID)).thenThrow(exception)

        // Act
        val outcome = testSubject.getAllServerIdByFolderId(folderId, accountId)

        // Assert
        assertThat(outcome).isEqualTo(Outcome.failure(MessageQueryError.UnhandledError(exception)))
    }

    @Test
    fun `getAllServerIdByFolderId should return AccountNotFound without touching the store when the account is unknown`() =
        runTest {
            // Arrange
            val unknownAccountId = AccountIdFactory.of(UNKNOWN_ACCOUNT_ID)

            // Act
            val outcome = testSubject.getAllServerIdByFolderId(folderId, unknownAccountId)

            // Assert
            assertThat(outcome).isEqualTo(Outcome.failure(MessageQueryError.AccountNotFound(unknownAccountId)))
            verifyNoInteractions(messageStore)
        }

    // endregion [getAllServerIdByFolderId]

    private fun stubKnownAccount() {
        whenever(accountManager.findById(account.id)).thenReturn(FakeData.legacyAccount)
    }
}

private class QueryTestMessageStoreFactory(
    private val messageStoresByUuid: Map<AccountId, ListenableMessageStore>,
) : MessageStoreFactory {
    override fun create(accountId: AccountId): ListenableMessageStore = messageStoresByUuid.getValue(accountId)
}
