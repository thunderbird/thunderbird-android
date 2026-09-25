package net.thunderbird.app.common.feature.mail.message.data.repository

import app.k9mail.legacy.mailstore.ListenableMessageStore
import app.k9mail.legacy.mailstore.MessageStore
import app.k9mail.legacy.mailstore.MessageStoreFactory
import app.k9mail.legacy.mailstore.MessageStoreManager
import app.k9mail.legacy.mailstore.SaveMessageData
import app.k9mail.legacy.message.extractors.PreviewResult
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.internet.MimeMessage
import com.fsck.k9.mailstore.SaveMessageDataCreator
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import net.thunderbird.app.common.feature.mail.FakeData
import net.thunderbird.components.core.outcome.Outcome
import net.thunderbird.core.android.account.AccountRemovedListener
import net.thunderbird.core.android.account.AccountsChangeListener
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.FolderId
import net.thunderbird.feature.mail.folder.LegacyFolderIdFactory
import net.thunderbird.feature.mail.folder.api.OutboxFolderManager
import net.thunderbird.feature.mail.message.LegacyMessageIdFactory
import net.thunderbird.feature.mail.message.Message
import net.thunderbird.feature.mail.message.MessageAddress
import net.thunderbird.feature.mail.message.MessageDownloadState
import net.thunderbird.feature.mail.message.MessageEnvelope
import net.thunderbird.feature.mail.message.MessageHeaderId
import net.thunderbird.feature.mail.message.MessageHeaders
import net.thunderbird.feature.mail.message.MessageId
import net.thunderbird.feature.mail.message.MessageServerId
import net.thunderbird.feature.mail.message.domain.GetMessageIdCriteria
import net.thunderbird.feature.mail.message.domain.MessageQueryError
import net.thunderbird.feature.mail.message.domain.MessageQueryRepository
import net.thunderbird.feature.mail.message.mapper.MessageDataMapper
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import com.fsck.k9.mail.Message as LegacyMessageDto
import com.fsck.k9.mail.MessageDownloadState as LegacyMessageDownloadState

private const val FOLDER_ID = 7L
private const val OUTBOX_FOLDER_ID = 3L
private const val EXISTING_MESSAGE_ID = 42L
private const val SAVED_MESSAGE_ID = 5L
private const val SERVER_ID = "100"

/**
 * [MessageStore] and [SaveMessageDataCreator] are mocked here: the store is a 60+ method legacy interface and the
 * creator is a concrete class wired to several legacy extractors. Only the calls the repository makes are stubbed.
 * Everything else uses fakes.
 */
class DefaultMessageLifecycleRepositoryTest {
    private val account: LegacyAccountDto = FakeData.legacyAccountDto
    private val accountId: AccountId = account.id
    private val folderId: FolderId = LegacyFolderIdFactory.of(FOLDER_ID)

    private val legacyMessage: LegacyMessageDto = MimeMessage.create()
    private val saveMessageData = SaveMessageData(
        message = legacyMessage,
        subject = null,
        date = 0L,
        internalDate = 0L,
        downloadState = LegacyMessageDownloadState.FULL,
        attachmentCount = 0,
        previewResult = PreviewResult.none(),
        encryptionType = null,
    )

    private val messageStore = mock<MessageStore>()
    private val saveMessageDataCreator = mock<SaveMessageDataCreator>()
    private val messageQueryRepository = FakeMessageQueryRepository()
    private val messageStoreManager = MessageStoreManager(
        accountManager = FakeLegacyAccountDtoManager(accounts = listOf(account)),
        messageStoreFactory = FakeMessageStoreFactory(
            messageStoresByUuid = mapOf(
                account.uuid to ListenableMessageStore(messageStore, mainImmediateDispatcher = Dispatchers.Unconfined),
            ),
        ),
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val testSubject = DefaultMessageLifecycleRepository(
        logger = TestLogger(),
        messageStoreManager = messageStoreManager,
        saveMessageDataCreator = saveMessageDataCreator,
        messageMapper = FakeMessageDataMapper(legacyMessage),
        messageIdLegacyEntityIdFactory = LegacyMessageIdFactory,
        folderIdLegacyEntityIdFactory = LegacyFolderIdFactory,
        outboxFolderManager = FakeOutboxFolderManager(outboxFolderId = OUTBOX_FOLDER_ID),
        ioDispatcher = UnconfinedTestDispatcher(),
    )

    // region create

    @Test
    fun `create should save a remote message when it does not exist yet`() = runTest {
        // Arrange
        val message = buildDomainMessage(serverId = MessageServerId(SERVER_ID))
        stubSaveMessageData()
        whenever(messageStore.saveRemoteMessage(FOLDER_ID, SERVER_ID, saveMessageData))
            .thenReturn(SAVED_MESSAGE_ID)

        // Act
        val outcome = testSubject.create(message, accountId, folderId)

        // Assert
        assertThat(outcome).isEqualTo(Outcome.success(LegacyMessageIdFactory.of(SAVED_MESSAGE_ID)))
    }

    @Test
    fun `create should save a local message in the given folder when the message has no server id`() = runTest {
        // Arrange
        val message = buildDomainMessage(serverId = null)
        stubSaveMessageData()
        whenever(messageStore.saveLocalMessage(FOLDER_ID, saveMessageData, null)).thenReturn(SAVED_MESSAGE_ID)

        // Act
        val outcome = testSubject.create(message, accountId, folderId)

        // Assert
        assertThat(outcome).isEqualTo(Outcome.success(LegacyMessageIdFactory.of(SAVED_MESSAGE_ID)))
        assertThat(messageQueryRepository.lastCriteria).isEqualTo(null)
        verify(messageStore).saveLocalMessage(FOLDER_ID, saveMessageData, null)
    }

    @Test
    fun `create should save a local message in the outbox when no folder id is given`() = runTest {
        // Arrange
        val message = buildDomainMessage(serverId = null)
        stubSaveMessageData()
        whenever(messageStore.saveLocalMessage(OUTBOX_FOLDER_ID, saveMessageData, null)).thenReturn(SAVED_MESSAGE_ID)

        // Act
        val outcome = testSubject.create(message, accountId, folderId = null)

        // Assert
        assertThat(outcome).isEqualTo(Outcome.success(LegacyMessageIdFactory.of(SAVED_MESSAGE_ID)))
        verify(messageStore).saveLocalMessage(OUTBOX_FOLDER_ID, saveMessageData, null)
    }

    @Test
    fun `create should map the domain download state onto the legacy download state`() = runTest {
        // Arrange
        val message = buildDomainMessage(serverId = null, downloadState = MessageDownloadState.PARTIAL)
        val partialSaveMessageData = saveMessageData.copy(downloadState = LegacyMessageDownloadState.PARTIAL)
        whenever(saveMessageDataCreator.createSaveMessageData(legacyMessage, LegacyMessageDownloadState.PARTIAL, null))
            .thenReturn(partialSaveMessageData)
        whenever(messageStore.saveLocalMessage(FOLDER_ID, partialSaveMessageData, null)).thenReturn(SAVED_MESSAGE_ID)

        // Act
        val outcome = testSubject.create(message, accountId, folderId)

        // Assert
        assertThat(outcome).isEqualTo(Outcome.success(LegacyMessageIdFactory.of(SAVED_MESSAGE_ID)))
        verify(saveMessageDataCreator).createSaveMessageData(legacyMessage, LegacyMessageDownloadState.PARTIAL, null)
    }

    // endregion

    private fun stubSaveMessageData() {
        whenever(saveMessageDataCreator.createSaveMessageData(legacyMessage, LegacyMessageDownloadState.FULL, null))
            .thenReturn(saveMessageData)
    }

    private fun buildDomainMessage(
        id: MessageId? = null,
        serverId: MessageServerId?,
        downloadState: MessageDownloadState = MessageDownloadState.FULL,
    ): Message = Message(
        id = id,
        serverId = serverId,
        accountId = accountId,
        folderId = null,
        threadRoot = null,
        receivedAt = LocalDateTime(2024, 1, 15, 10, 30, 0),
        envelope = MessageEnvelope(
            subject = "Test subject",
            from = listOf(MessageAddress(value = "alice@example.com", label = "Alice")),
            sender = null,
            replyTo = emptyList(),
            to = listOf(MessageAddress(value = "bob@example.com", label = "Bob")),
            cc = emptyList(),
            bcc = emptyList(),
            sentAt = LocalDateTime(2024, 1, 15, 10, 25, 0),
        ),
        headers = MessageHeaders(
            messageId = MessageHeaderId("<msg1@example.com>"),
            references = emptyList(),
            inReplyTo = emptyList(),
            extra = emptyMap(),
        ),
        body = null,
        downloadState = downloadState,
    )
}

private class FakeLegacyAccountDtoManager(
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

private class FakeMessageStoreFactory(
    private val messageStoresByUuid: Map<String, ListenableMessageStore>,
) : MessageStoreFactory {
    override fun create(account: LegacyAccountDto): ListenableMessageStore = messageStoresByUuid.getValue(account.uuid)
}

private class FakeMessageQueryRepository : MessageQueryRepository {
    var result: Outcome<MessageId?, MessageQueryError> = Outcome.success(null)
    var lastCriteria: GetMessageIdCriteria? = null
        private set

    override suspend fun findIdByCriteria(
        accountId: AccountId,
        criteria: GetMessageIdCriteria,
    ): Outcome<MessageId?, MessageQueryError> {
        lastCriteria = criteria
        return result
    }
}

private class FakeMessageDataMapper(
    private val legacyMessage: LegacyMessageDto,
) : MessageDataMapper<LegacyMessageDto> {
    override suspend fun toDomain(dto: LegacyMessageDto): Message = error("Not used by these tests")
    override suspend fun toDto(domain: Message): LegacyMessageDto = legacyMessage
}

private class FakeOutboxFolderManager(
    private val outboxFolderId: Long,
) : OutboxFolderManager {
    override suspend fun getOutboxFolderId(accountId: AccountId, createIfMissing: Boolean): Long = outboxFolderId

    override suspend fun createOutboxFolder(accountId: AccountId): Outcome<Long, Exception> =
        error("Not used by these tests")

    override suspend fun hasPendingMessages(accountId: AccountId): Boolean = error("Not used by these tests")
}
