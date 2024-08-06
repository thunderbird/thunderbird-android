package com.fsck.k9.mailstore

import app.k9mail.legacy.mailstore.ListenableMessageStore
import app.k9mail.legacy.mailstore.MessageDetailsAccessor
import app.k9mail.legacy.mailstore.MessageMapper
import app.k9mail.legacy.mailstore.MessageStoreManager
import app.k9mail.legacy.message.extractors.PreviewResult
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.Flag
import java.util.UUID
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

private const val MESSAGE_ID = 1L
private const val MESSAGE_ID_2 = 2L
private const val MESSAGE_ID_3 = 3L
private const val FOLDER_ID = 20L
private const val FOLDER_ID_2 = 21L
private const val THREAD_ROOT = 30L
private const val THREAD_ROOT_2 = 31L

private const val SELECTION = "irrelevant"
private val SELECTION_ARGS = arrayOf("irrelevant")
private const val SORT_ORDER = "irrelevant"

class MessageListRepositoryTest {
    private val accountUuid = UUID.randomUUID().toString()

    private val messageStore = mock<ListenableMessageStore>()
    private val messageStoreManager = mock<MessageStoreManager> {
        on { getMessageStore(accountUuid) } doReturn messageStore
    }

    private val messageListRepository = MessageListRepository(messageStoreManager)

    @Before
    fun setUp() {
        startKoin {
            modules(
                module {
                    single { messageListRepository }
                },
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `adding and removing listener`() {
        var messageListChanged = 0
        val listener = MessageListChangedListener {
            messageListChanged++
        }
        messageListRepository.addListener(accountUuid, listener)

        messageListRepository.notifyMessageListChanged(accountUuid)

        assertThat(messageListChanged).isEqualTo(1)

        messageListRepository.removeListener(listener)

        messageListRepository.notifyMessageListChanged(accountUuid)

        assertThat(messageListChanged).isEqualTo(1)
    }

    @Test
    fun `only notify listener when account UUID matches`() {
        var messageListChanged = 0
        val listener = MessageListChangedListener {
            messageListChanged++
        }
        messageListRepository.addListener(accountUuid, listener)

        messageListRepository.notifyMessageListChanged("otherAccountUuid")

        assertThat(messageListChanged).isEqualTo(0)
    }

    @Test
    fun `notifyMessageListChanged() without any listeners should not throw`() {
        messageListRepository.notifyMessageListChanged(accountUuid)
    }

    @Test
    fun `getMessages() should use flag values from the cache`() {
        addMessages(
            MessageData(
                messageId = MESSAGE_ID,
                folderId = FOLDER_ID,
                threadRoot = THREAD_ROOT,
                isRead = false,
                isStarred = true,
                isAnswered = false,
                isForwarded = true,
            ),
        )
        MessageListCache.getCache(accountUuid).apply {
            setFlagForMessages(listOf(MESSAGE_ID), Flag.SEEN, true)
            setValueForThreads(listOf(THREAD_ROOT), Flag.FLAGGED, false)
        }

        val result = messageListRepository.getMessages(accountUuid, SELECTION, SELECTION_ARGS, SORT_ORDER) { message ->
            MessageData(
                messageId = message.id,
                folderId = message.folderId,
                threadRoot = message.threadRoot,
                isRead = message.isRead,
                isStarred = message.isStarred,
                isAnswered = message.isAnswered,
                isForwarded = message.isForwarded,
            )
        }

        assertThat(result).containsExactly(
            MessageData(
                messageId = MESSAGE_ID,
                folderId = FOLDER_ID,
                threadRoot = THREAD_ROOT,
                isRead = true,
                isStarred = false,
                isAnswered = false,
                isForwarded = true,
            ),
        )
    }

    @Test
    fun `getMessages() should skip messages marked as hidden in the cache`() {
        addMessages(
            MessageData(messageId = MESSAGE_ID, folderId = FOLDER_ID, threadRoot = THREAD_ROOT),
            MessageData(messageId = MESSAGE_ID_2, folderId = FOLDER_ID, threadRoot = THREAD_ROOT),
        )
        hideMessage(MESSAGE_ID, FOLDER_ID)

        val result = messageListRepository.getMessages(accountUuid, SELECTION, SELECTION_ARGS, SORT_ORDER) { message ->
            message.id
        }

        assertThat(result).containsExactly(MESSAGE_ID_2)
    }

    @Test
    fun `getMessages() should not skip message when marked as hidden in a different folder`() {
        addMessages(
            MessageData(messageId = MESSAGE_ID, folderId = FOLDER_ID, threadRoot = THREAD_ROOT),
            MessageData(messageId = MESSAGE_ID_2, folderId = FOLDER_ID, threadRoot = THREAD_ROOT),
        )
        hideMessage(MESSAGE_ID, FOLDER_ID_2)

        val result = messageListRepository.getMessages(accountUuid, SELECTION, SELECTION_ARGS, SORT_ORDER) { message ->
            message.id
        }

        assertThat(result).containsExactly(MESSAGE_ID, MESSAGE_ID_2)
    }

    @Test
    fun `getThreadedMessages() should use flag values from the cache`() {
        addThreadedMessages(
            MessageData(
                messageId = MESSAGE_ID,
                folderId = FOLDER_ID,
                threadRoot = THREAD_ROOT,
                isRead = false,
                isStarred = true,
                isAnswered = false,
                isForwarded = true,
            ),
        )
        MessageListCache.getCache(accountUuid).apply {
            setFlagForMessages(listOf(MESSAGE_ID), Flag.SEEN, true)
            setValueForThreads(listOf(THREAD_ROOT), Flag.FLAGGED, false)
        }

        val result = messageListRepository.getThreadedMessages(
            accountUuid,
            SELECTION,
            SELECTION_ARGS,
            SORT_ORDER,
        ) { message ->
            MessageData(
                messageId = message.id,
                folderId = message.folderId,
                threadRoot = message.threadRoot,
                isRead = message.isRead,
                isStarred = message.isStarred,
                isAnswered = message.isAnswered,
                isForwarded = message.isForwarded,
            )
        }

        assertThat(result).containsExactly(
            MessageData(
                messageId = MESSAGE_ID,
                folderId = FOLDER_ID,
                threadRoot = THREAD_ROOT,
                isRead = true,
                isStarred = false,
                isAnswered = false,
                isForwarded = true,
            ),
        )
    }

    @Test
    fun `getThreadedMessages() should skip messages marked as hidden in the cache`() {
        addThreadedMessages(
            MessageData(messageId = MESSAGE_ID, folderId = FOLDER_ID, threadRoot = THREAD_ROOT),
            MessageData(messageId = MESSAGE_ID_2, folderId = FOLDER_ID, threadRoot = THREAD_ROOT_2),
        )
        hideMessage(MESSAGE_ID, FOLDER_ID)

        val result = messageListRepository.getThreadedMessages(
            accountUuid,
            SELECTION,
            SELECTION_ARGS,
            SORT_ORDER,
        ) { message ->
            message.id
        }

        assertThat(result).containsExactly(MESSAGE_ID_2)
    }

    @Test
    fun `getThreadedMessages() should not skip message when marked as hidden in a different folder`() {
        addThreadedMessages(
            MessageData(messageId = MESSAGE_ID, folderId = FOLDER_ID, threadRoot = THREAD_ROOT),
            MessageData(messageId = MESSAGE_ID_2, folderId = FOLDER_ID, threadRoot = THREAD_ROOT_2),
        )
        hideMessage(MESSAGE_ID, FOLDER_ID_2)

        val result = messageListRepository.getThreadedMessages(
            accountUuid,
            SELECTION,
            SELECTION_ARGS,
            SORT_ORDER,
        ) { message ->
            message.id
        }

        assertThat(result).containsExactly(MESSAGE_ID, MESSAGE_ID_2)
    }

    @Test
    fun `getThread() should use flag values from the cache`() {
        addMessagesToThread(
            THREAD_ROOT,
            MessageData(
                messageId = MESSAGE_ID,
                folderId = FOLDER_ID,
                threadRoot = THREAD_ROOT,
                isRead = false,
                isStarred = true,
                isAnswered = false,
                isForwarded = true,
            ),
            MessageData(
                messageId = MESSAGE_ID_2,
                folderId = FOLDER_ID,
                threadRoot = THREAD_ROOT,
                isRead = false,
                isStarred = true,
                isAnswered = true,
                isForwarded = false,
            ),
        )
        MessageListCache.getCache(accountUuid).apply {
            setFlagForMessages(listOf(MESSAGE_ID), Flag.SEEN, true)
            setValueForThreads(listOf(THREAD_ROOT), Flag.FLAGGED, false)
        }

        val result = messageListRepository.getThread(
            accountUuid,
            THREAD_ROOT,
            SORT_ORDER,
        ) { message ->
            MessageData(
                messageId = message.id,
                folderId = message.folderId,
                threadRoot = message.threadRoot,
                isRead = message.isRead,
                isStarred = message.isStarred,
                isAnswered = message.isAnswered,
                isForwarded = message.isForwarded,
            )
        }

        assertThat(result).containsExactly(
            MessageData(
                messageId = MESSAGE_ID,
                folderId = FOLDER_ID,
                threadRoot = THREAD_ROOT,
                isRead = true,
                isStarred = false,
                isAnswered = false,
                isForwarded = true,
            ),
            MessageData(
                messageId = MESSAGE_ID_2,
                folderId = FOLDER_ID,
                threadRoot = THREAD_ROOT,
                isRead = false,
                isStarred = false,
                isAnswered = true,
                isForwarded = false,
            ),
        )
    }

    @Test
    fun `getThread() should skip messages marked as hidden in the cache`() {
        addMessagesToThread(
            THREAD_ROOT,
            MessageData(messageId = MESSAGE_ID, folderId = FOLDER_ID, threadRoot = THREAD_ROOT),
            MessageData(messageId = MESSAGE_ID_2, folderId = FOLDER_ID, threadRoot = THREAD_ROOT),
            MessageData(messageId = MESSAGE_ID_3, folderId = FOLDER_ID, threadRoot = THREAD_ROOT),
        )
        hideMessage(MESSAGE_ID, FOLDER_ID)

        val result = messageListRepository.getThread(accountUuid, THREAD_ROOT, SORT_ORDER) { message -> message.id }

        assertThat(result).containsExactly(MESSAGE_ID_2, MESSAGE_ID_3)
    }

    @Test
    fun `getThread() should not skip message when marked as hidden in a different folder`() {
        addMessagesToThread(
            THREAD_ROOT,
            MessageData(messageId = MESSAGE_ID, folderId = FOLDER_ID, threadRoot = THREAD_ROOT),
            MessageData(messageId = MESSAGE_ID_2, folderId = FOLDER_ID, threadRoot = THREAD_ROOT),
            MessageData(messageId = MESSAGE_ID_3, folderId = FOLDER_ID, threadRoot = THREAD_ROOT),
        )
        hideMessage(MESSAGE_ID, FOLDER_ID_2)

        val result = messageListRepository.getThread(accountUuid, THREAD_ROOT, SORT_ORDER) { message -> message.id }

        assertThat(result).containsExactly(MESSAGE_ID, MESSAGE_ID_2, MESSAGE_ID_3)
    }

    private fun addMessages(vararg messages: MessageData) {
        messageStore.stub {
            on { getMessages<Any>(eq(SELECTION), eq(SELECTION_ARGS), eq(SORT_ORDER), any()) } doAnswer {
                val mapper: MessageMapper<Any?> = it.getArgument(3)

                runMessageMapper(messages, mapper)
            }
        }
    }

    private fun addThreadedMessages(vararg messages: MessageData) {
        messageStore.stub {
            on { getThreadedMessages<Any>(eq(SELECTION), eq(SELECTION_ARGS), eq(SORT_ORDER), any()) } doAnswer {
                val mapper: MessageMapper<Any?> = it.getArgument(3)

                runMessageMapper(messages, mapper)
            }
        }
    }

    @Suppress("SameParameterValue")
    private fun addMessagesToThread(threadRoot: Long, vararg messages: MessageData) {
        messageStore.stub {
            on { getThread<Any>(eq(threadRoot), eq(SORT_ORDER), any()) } doAnswer {
                val mapper: MessageMapper<Any?> = it.getArgument(2)

                runMessageMapper(messages, mapper)
            }
        }
    }

    private fun runMessageMapper(messages: Array<out MessageData>, mapper: MessageMapper<Any?>): List<Any> {
        return messages.mapNotNull { message ->
            mapper.map(
                object : MessageDetailsAccessor {
                    override val id = message.messageId
                    override val messageServerId = "irrelevant"
                    override val folderId = message.folderId
                    override val fromAddresses = emptyList<Address>()
                    override val toAddresses = emptyList<Address>()
                    override val ccAddresses = emptyList<Address>()
                    override val messageDate = 0L
                    override val internalDate = 0L
                    override val subject = "irrelevant"
                    override val preview = PreviewResult.error()
                    override val isRead = message.isRead
                    override val isStarred = message.isStarred
                    override val isAnswered = message.isAnswered
                    override val isForwarded = message.isForwarded
                    override val hasAttachments = false
                    override val threadRoot = message.threadRoot
                    override val threadCount = 0
                },
            )
        }
    }

    @Suppress("SameParameterValue")
    private fun hideMessage(messageId: Long, folderId: Long) {
        val cache = MessageListCache.getCache(accountUuid)

        val localFolder = mock<LocalFolder> {
            on { databaseId } doReturn folderId
        }

        val localMessage = mock<LocalMessage> {
            on { databaseId } doReturn messageId
            on { folder } doReturn localFolder
        }

        cache.hideMessages(listOf(localMessage))
    }
}

private data class MessageData(
    val messageId: Long,
    val folderId: Long,
    val threadRoot: Long,
    val isRead: Boolean = false,
    val isStarred: Boolean = false,
    val isAnswered: Boolean = false,
    val isForwarded: Boolean = false,
)
