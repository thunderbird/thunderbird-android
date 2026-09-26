package com.fsck.k9.mailstore

import android.database.sqlite.SQLiteDatabase
import androidx.core.content.contentValuesOf
import app.k9mail.legacy.mailstore.MessageStore
import app.k9mail.legacy.mailstore.MessageStoreManager
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasMessage
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import assertk.assertions.prop
import assertk.assertions.single
import com.fsck.k9.K9RobolectricTest
import com.fsck.k9.Preferences
import com.fsck.k9.backend.api.BackendFolder
import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.backend.api.updateFolders
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.FolderType
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.MessageDownloadState
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.internet.MimeMessage
import com.fsck.k9.mail.internet.MimeMessageHelper
import com.fsck.k9.mail.internet.TextBody
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import net.thunderbird.components.core.outcome.Outcome
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.common.mail.Flag
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.FolderId
import net.thunderbird.feature.mail.folder.LegacyFolderIdFactory
import net.thunderbird.feature.mail.message.LegacyMessageIdFactory
import net.thunderbird.feature.mail.message.MessageEnvelope
import net.thunderbird.feature.mail.message.MessageFlag
import net.thunderbird.feature.mail.message.MessageHeaderId
import net.thunderbird.feature.mail.message.MessageHeaders
import net.thunderbird.feature.mail.message.MessageServerId
import net.thunderbird.feature.mail.message.domain.GetMessageIdCriteria
import net.thunderbird.feature.mail.message.domain.MessageLifecycleError
import net.thunderbird.feature.mail.message.domain.MessageLifecycleRepository
import net.thunderbird.feature.mail.message.domain.MessageQueryError
import net.thunderbird.feature.mail.message.domain.MessageQueryRepository
import net.thunderbird.feature.mail.message.mapper.MessageDataMapper
import org.junit.After
import org.junit.Test
import org.koin.core.component.inject
import net.thunderbird.feature.mail.message.Message as DomainMessage
import net.thunderbird.feature.mail.message.MessageDownloadState as DomainMessageDownloadState
import net.thunderbird.feature.mail.message.MessageId as DomainMessageId

class K9BackendFolderTest : K9RobolectricTest() {
    val preferences: Preferences by inject()
    val localStoreProvider: LocalStoreProvider by inject()
    val messageStoreManager: MessageStoreManager by inject()
    val saveMessageDataCreator: SaveMessageDataCreator by inject()

    val account: LegacyAccountDto = createAccount()
    val messageStore: MessageStore = messageStoreManager.getMessageStore(account)
    private val messageLifecycleRepository = FakeMessageLifecycleRepository()
    private val messageQueryRepository = FakeMessageQueryRepository()
    val backendFolder = createBackendFolder()
    val database: LockableDatabase = localStoreProvider.getInstance(account).database

    @After
    fun tearDown() {
        preferences.deleteAccount(account)
    }

    @Test
    fun getMessageFlags() = runTest {
        val flags = setOf(Flag.SEEN, Flag.DRAFT, Flag.X_DOWNLOADED_FULL)
        createMessageInBackendFolder(MESSAGE_SERVER_ID, flags)

        val messageFlags = backendFolder.getMessageFlags(MESSAGE_SERVER_ID)

        assertThat(messageFlags).isEqualTo(flags)
    }

    @Test
    fun getMessageFlags_withFlagsColumnSetToNull_shouldBeTreatedAsEmpty() = runTest {
        createMessageInBackendFolder(MESSAGE_SERVER_ID)
        setFlagsColumnToNull()

        val messageFlags = backendFolder.getMessageFlags(MESSAGE_SERVER_ID)

        assertThat(messageFlags.isEmpty()).isTrue()
    }

    @Test
    fun getMessageFlags_withFlagsColumnSetToNull_shouldReadSpecialColumnFlags() = runTest {
        val flags = setOf(Flag.SEEN, Flag.FLAGGED, Flag.ANSWERED, Flag.FORWARDED)
        createMessageInBackendFolder(MESSAGE_SERVER_ID, flags)
        setFlagsColumnToNull()

        val messageFlags = backendFolder.getMessageFlags(MESSAGE_SERVER_ID)

        assertThat(messageFlags).isEqualTo(flags)
    }

    @Test
    fun saveCompleteMessage_withoutServerId_shouldThrow() = runTest {
        val message = createMessage(messageServerId = null)

        assertFailure {
            backendFolder.saveMessage(message, MessageDownloadState.FULL)
        }.isInstanceOf<IllegalStateException>()
            .hasMessage("Message requires a server ID to be set")
    }

    @Test
    fun savePartialMessage_withoutServerId_shouldThrow() = runTest {
        val message = createMessage(messageServerId = null)

        assertFailure {
            backendFolder.saveMessage(message, MessageDownloadState.PARTIAL)
        }.isInstanceOf<IllegalStateException>()
            .hasMessage("Message requires a server ID to be set")
    }

    @Test
    fun saveMessage_withNewMessage_shouldCreateMessageViaLifecycleRepository() = runTest {
        val message = createMessage(MESSAGE_SERVER_ID)

        backendFolder.saveMessage(message, MessageDownloadState.FULL)

        assertThat(messageLifecycleRepository.createdMessages).single()
            .prop(DomainMessage::serverId)
            .isEqualTo(MessageServerId(MESSAGE_SERVER_ID))
    }

    @Test
    fun destroyMessages_shouldDestroyTheGivenMessagesViaLifecycleRepository() = runTest {
        backendFolder.destroyMessages(listOf("msg001", "msg003"))

        assertThat(messageLifecycleRepository.destroyedServerIds).isEqualTo(
            listOf(setOf(MessageServerId("msg001"), MessageServerId("msg003"))),
        )
    }

    @Test
    fun destroyMessages_whenRepositoryFails_shouldThrowMessagingException() = runTest {
        val cause = IllegalStateException("database is locked")
        messageLifecycleRepository.destroyError = MessageLifecycleError.UnhandledError(cause)

        assertFailure {
            backendFolder.destroyMessages(listOf("msg001"))
        }.isInstanceOf<MessagingException>()
            .transform { it.cause }
            .isEqualTo(cause)
    }

    @Test
    fun clearAllMessages_shouldDestroyEveryServerIdReturnedByQueryRepository() = runTest {
        val serverIds = setOf(MessageServerId("msg001"), MessageServerId("msg002"))
        messageQueryRepository.serverIds = serverIds

        backendFolder.clearAllMessages()

        assertThat(messageLifecycleRepository.destroyedServerIds).isEqualTo(listOf(serverIds))
    }

    @Test
    fun clearAllMessages_onEmptyFolder_shouldNotFail() = runTest {
        backendFolder.clearAllMessages()

        assertThat(messageLifecycleRepository.destroyedServerIds).isEqualTo(listOf(emptySet()))
    }

    @Test
    fun clearAllMessages_whenQueryFails_shouldThrowMessagingExceptionWithoutDestroying() = runTest {
        val cause = IllegalStateException("database is locked")
        messageQueryRepository.queryError = MessageQueryError.UnhandledError(cause)

        assertFailure {
            backendFolder.clearAllMessages()
        }.isInstanceOf<MessagingException>()
            .transform { it.cause }
            .isEqualTo(cause)
        assertThat(messageLifecycleRepository.destroyedServerIds).isEmpty()
    }

    @Test
    fun clearAllMessages_whenRepositoryFails_shouldThrowMessagingException() = runTest {
        messageQueryRepository.serverIds = setOf(MessageServerId("msg001"))
        val cause = IllegalStateException("database is locked")
        messageLifecycleRepository.destroyError = MessageLifecycleError.UnhandledError(cause)

        assertFailure {
            backendFolder.clearAllMessages()
        }.isInstanceOf<MessagingException>()
            .transform { it.cause }
            .isEqualTo(cause)
    }

    fun createAccount(): LegacyAccountDto {
        // FIXME: This is a hack to get Preferences into a state where it's safe to call newAccount()
        preferences.clearAccounts()
        return preferences.newAccount().apply {
            incomingServerSettings = SERVER_SETTINGS
            outgoingServerSettings = SERVER_SETTINGS
        }
    }

    fun createBackendFolder(): BackendFolder {
        val backendStorage = K9BackendStorage(
            logger = TestLogger(),
            messageStore = messageStore,
            folderSettingsProvider = createFolderSettingsProvider(),
            listeners = emptyList(),
            messageQueryRepository = messageQueryRepository,
            messageLifecycleRepository = messageLifecycleRepository,
            folderIdLegacyEntityIdFactory = LegacyFolderIdFactory,
            messageDataMapper = FakeMessageDataMapper(),
        )
        backendStorage.updateFolders {
            createFolders(listOf(FolderInfo(FOLDER_SERVER_ID, FOLDER_NAME, FOLDER_TYPE)))
        }

        val folderServerIds = backendStorage.getFolderServerIds()
        assertThat(folderServerIds).contains(FOLDER_SERVER_ID)

        return backendStorage.getFolder(FOLDER_SERVER_ID)
    }

    fun createMessageInBackendFolder(messageServerId: String, flags: Set<Flag> = emptySet()) {
        val message = createMessage(messageServerId, flags)
        val folderId = messageStore.getFolderId(FOLDER_SERVER_ID) ?: error("Couldn't find folder $FOLDER_SERVER_ID")
        val messageData = saveMessageDataCreator.createSaveMessageData(message, MessageDownloadState.FULL)
        messageStore.saveRemoteMessage(folderId, messageServerId, messageData)

        val messageServerIds = backendFolder.getMessageServerIds()
        assertThat(messageServerIds).contains(messageServerId)
    }

    private fun createMessage(messageServerId: String?, flags: Set<Flag> = emptySet()): Message {
        return MimeMessage().apply {
            subject = "Test message"
            setFrom(Address("alice@domain.example"))
            setHeader("To", "bob@domain.example")
            MimeMessageHelper.setBody(this, TextBody("Hello Bob!"))

            uid = messageServerId
            setFlags(flags, true)
        }
    }

    private fun setFlagsColumnToNull() {
        dbOperation { db ->
            val numberOfUpdatedRows = db.update(
                "messages",
                contentValuesOf("flags" to null),
                "uid = ?",
                arrayOf(MESSAGE_SERVER_ID),
            )
            assertThat(numberOfUpdatedRows).isEqualTo(1)
        }
    }

    private fun dbOperation(action: (SQLiteDatabase) -> Unit) = database.execute(false, action)

    companion object {
        const val FOLDER_SERVER_ID = "testFolder"
        const val FOLDER_NAME = "Test Folder"
        val FOLDER_TYPE = FolderType.INBOX
        const val MESSAGE_SERVER_ID = "msg001"

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

/**
 * A minimal [MessageDataMapper] good enough for [K9BackendFolderTest]: it maps the server id,
 * subject and the general (non-download-state) flags of the legacy [Message] to the domain model.
 *
 * The real mapper (`DefaultMessageDataMapper`) lives in `:app-common`, which depends on this
 * module, so it can't be reused here.
 */
private class FakeMessageDataMapper : MessageDataMapper<Message> {
    @OptIn(ExperimentalUuidApi::class)
    override suspend fun toDomain(dto: Message): DomainMessage {
        val epoch = LocalDateTime(1970, 1, 1, 0, 0)
        return DomainMessage(
            id = null,
            serverId = dto.uid?.let(::MessageServerId),
            accountId = AccountId(Uuid.random()),
            folderId = null,
            threadRoot = null,
            receivedAt = epoch,
            envelope = MessageEnvelope(
                subject = dto.subject,
                from = emptyList(),
                sender = null,
                replyTo = emptyList(),
                to = emptyList(),
                cc = emptyList(),
                bcc = emptyList(),
                sentAt = epoch,
            ),
            headers = MessageHeaders(
                messageId = MessageHeaderId(dto.messageId.orEmpty()),
                references = null,
                inReplyTo = emptyList(),
                extra = emptyMap(),
            ),
            body = null,
            downloadState = DomainMessageDownloadState.ENVELOPE,
            flags = dto.toDomainFlags(),
        )
    }

    override suspend fun toDto(domain: DomainMessage): Message = error("Not used by these tests")

    private fun Message.toDomainFlags(): Set<MessageFlag> = buildSet {
        if (isSet(Flag.SEEN)) add(MessageFlag.Read)
        if (isSet(Flag.FLAGGED)) add(MessageFlag.Starred)
        if (isSet(Flag.ANSWERED)) add(MessageFlag.Answered)
        if (isSet(Flag.FORWARDED)) add(MessageFlag.Forwarded)
        if (isSet(Flag.DRAFT)) add(MessageFlag.Draft)
        if (isSet(Flag.DELETED)) add(MessageFlag.Deleted)
    }
}

private class FakeMessageLifecycleRepository : MessageLifecycleRepository {
    var createResult: Outcome<DomainMessageId, MessageLifecycleError> = Outcome.success(LegacyMessageIdFactory.of(1L))
    val createdMessages = mutableListOf<DomainMessage>()
    val destroyedServerIds = mutableListOf<Set<MessageServerId>>()
    var destroyError: MessageLifecycleError? = null

    override suspend fun create(
        message: DomainMessage,
        accountId: AccountId,
        folderId: FolderId?,
    ): Outcome<DomainMessageId, MessageLifecycleError> {
        createdMessages += message
        return createResult
    }

    override suspend fun update(
        message: DomainMessage,
        accountId: AccountId,
        folderId: FolderId,
    ): Outcome<DomainMessageId, MessageLifecycleError> = error("Not used by these tests")

    override suspend fun move(
        messageId: DomainMessageId,
        destinationFolderId: FolderId,
        accountId: AccountId,
    ): Outcome<DomainMessageId, MessageLifecycleError> = error("Not used by these tests")

    override suspend fun moveAll(
        messageIds: List<DomainMessageId>,
        destinationFolderId: FolderId,
        accountId: AccountId,
    ): Outcome<Map<DomainMessageId, DomainMessageId>, MessageLifecycleError> = error("Not used by these tests")

    override suspend fun copy(
        messageId: DomainMessageId,
        destinationFolderId: FolderId,
        accountId: AccountId,
    ): Outcome<DomainMessageId, MessageLifecycleError> = error("Not used by these tests")

    override suspend fun copyAll(
        messageIds: List<DomainMessageId>,
        destinationFolderId: FolderId,
        accountId: AccountId,
    ): Outcome<Map<DomainMessageId, DomainMessageId>, MessageLifecycleError> = error("Not used by these tests")

    override suspend fun destroyAllByServerId(
        serverIds: Collection<MessageServerId>,
        folderId: FolderId,
        accountId: AccountId,
    ): Outcome<Unit, MessageLifecycleError> {
        destroyedServerIds += serverIds.toSet()
        return destroyError?.let { Outcome.failure(it) } ?: Outcome.success(Unit)
    }

    override suspend fun destroyByServerId(
        serverId: MessageServerId,
        folderId: FolderId,
        accountId: AccountId,
    ): Outcome<Unit, MessageLifecycleError> = destroyAllByServerId(listOf(serverId), folderId, accountId)
}

private class FakeMessageQueryRepository : MessageQueryRepository {
    var findIdResult: Outcome<DomainMessageId?, MessageQueryError> = Outcome.success(null)
    var serverIds: Set<MessageServerId> = emptySet()
    var queryError: MessageQueryError? = null

    override suspend fun findIdByCriteria(
        accountId: AccountId,
        criteria: GetMessageIdCriteria,
    ): Outcome<DomainMessageId?, MessageQueryError> = findIdResult

    override suspend fun getAllServerIdByFolderId(
        folderId: FolderId,
        accountId: AccountId,
    ): Outcome<Set<MessageServerId>, MessageQueryError> {
        return queryError?.let { Outcome.failure(it) } ?: Outcome.success(serverIds)
    }
}
