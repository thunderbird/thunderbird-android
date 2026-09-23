package net.thunderbird.app.common.feature.mail.message.data.repository

import app.k9mail.legacy.mailstore.ListenableMessageStore
import app.k9mail.legacy.mailstore.MessageStore
import app.k9mail.legacy.mailstore.MessageStoreFactory
import app.k9mail.legacy.mailstore.MessageStoreManager
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.mailstore.LocalStoreProvider
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.thunderbird.app.common.feature.mail.FakeData
import net.thunderbird.components.core.outcome.Outcome
import net.thunderbird.core.android.account.AccountRemovedListener
import net.thunderbird.core.android.account.AccountsChangeListener
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.mail.folder.FolderId
import net.thunderbird.feature.mail.folder.LegacyFolderIdFactory
import net.thunderbird.feature.mail.message.LegacyMessageIdFactory
import net.thunderbird.feature.mail.message.MessageServerId
import net.thunderbird.feature.mail.message.domain.GetMessageIdCriteria
import net.thunderbird.feature.mail.message.domain.MessageQueryError
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
    private val account: LegacyAccountDto = FakeData.legacyAccountDto
    private val accountId: AccountId = account.id
    private val folderId: FolderId = LegacyFolderIdFactory.of(FOLDER_ID)

    private val messageStore = mock<MessageStore>()
    private val accountManager = mock<LegacyAccountManager>()
    private val localStoreProvider = mock<LocalStoreProvider>()
    private val messageStoreManager = MessageStoreManager(
        accountManager = QueryTestLegacyAccountDtoManager(accounts = listOf(account)),
        messageStoreFactory = QueryTestMessageStoreFactory(
            messageStoresByUuid = mapOf(
                account.uuid to ListenableMessageStore(messageStore, mainImmediateDispatcher = Dispatchers.Unconfined),
            ),
        ),
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val testSubject = DefaultMessageQueryRepository(
        logger = TestLogger(),
        accountManager = accountManager,
        localStoreProvider = localStoreProvider,
        messageStoreManager = messageStoreManager,
        messageIdLegacyEntityIdFactory = LegacyMessageIdFactory,
        folderIdLegacyEntityIdFactory = LegacyFolderIdFactory,
        ioDispatcher = UnconfinedTestDispatcher(),
    )

    // region [findIdByCriteria]

    @Test
    fun `findIdByCriteria should return AccountNotFound without touching the store when the account is unknown`() =
        runTest {
            // Arrange
            val unknownAccountId = AccountIdFactory.of(UNKNOWN_ACCOUNT_ID)
            val criteria = GetMessageIdCriteria(folderId, MessageServerId(SERVER_ID))

            // Act
            val outcome = testSubject.findIdByCriteria(unknownAccountId, criteria)

            // Assert
            assertThat(outcome).isEqualTo(Outcome.failure(MessageQueryError.AccountNotFound(unknownAccountId)))
            verifyNoInteractions(messageStore, localStoreProvider)
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
        whenever(accountManager.getAccount(account.uuid)).thenReturn(FakeData.legacyAccount)
    }
}

private class QueryTestLegacyAccountDtoManager(
    accounts: List<LegacyAccountDto>,
) : LegacyAccountDtoManager {
    private val accountsByUuid = accounts.associateBy { it.uuid }

    override fun getAccounts(): List<LegacyAccountDto> = accountsByUuid.values.toList()
    override fun getAccountsFlow(): Flow<List<LegacyAccountDto>> = flowOf(getAccounts())
    override fun getAccount(accountUuid: String): LegacyAccountDto? = accountsByUuid[accountUuid]
    override fun getAccountFlow(accountUuid: String): Flow<LegacyAccountDto?> = flowOf(getAccount(accountUuid))
    override fun addAccountRemovedListener(listener: AccountRemovedListener) = Unit
    override fun moveAccount(account: LegacyAccountDto, newPosition: Int) = Unit
    override fun addOnAccountsChangeListener(accountsChangeListener: AccountsChangeListener) = Unit
    override fun removeOnAccountsChangeListener(accountsChangeListener: AccountsChangeListener) = Unit
    override fun saveAccount(account: LegacyAccountDto) = Unit
}

private class QueryTestMessageStoreFactory(
    private val messageStoresByUuid: Map<String, ListenableMessageStore>,
) : MessageStoreFactory {
    override fun create(account: LegacyAccountDto): ListenableMessageStore = messageStoresByUuid.getValue(account.uuid)
}
