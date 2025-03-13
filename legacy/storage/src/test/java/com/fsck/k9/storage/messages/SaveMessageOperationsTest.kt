package com.fsck.k9.storage.messages

import app.k9mail.legacy.mailstore.SaveMessageData
import app.k9mail.legacy.message.extractors.PreviewResult
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.startsWith
import com.fsck.k9.K9
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.MessageDownloadState
import com.fsck.k9.mail.Multipart
import com.fsck.k9.mail.Part
import com.fsck.k9.mail.testing.message.buildMessage
import com.fsck.k9.mailstore.StorageFilesProvider
import com.fsck.k9.message.extractors.BasicPartInfoExtractor
import com.fsck.k9.storage.RobolectricTest
import java.io.ByteArrayOutputStream
import java.util.Stack
import org.junit.After
import org.junit.Test

class SaveMessageOperationsTest : RobolectricTest() {
    private val messagePartDirectory = createRandomTempDirectory()
    private val sqliteDatabase = createDatabase()
    private val storageFilesProvider = object : StorageFilesProvider {
        override fun getDatabaseFile() = error("Not implemented")
        override fun getAttachmentDirectory() = messagePartDirectory
    }
    private val lockableDatabase = createLockableDatabaseMock(sqliteDatabase)
    private val attachmentFileManager = AttachmentFileManager(storageFilesProvider)
    private val basicPartInfoExtractor = BasicPartInfoExtractor()
    private val threadMessageOperations = ThreadMessageOperations()
    private val saveMessageOperations = SaveMessageOperations(
        lockableDatabase,
        attachmentFileManager,
        basicPartInfoExtractor,
        threadMessageOperations,
    )

    @After
    fun tearDown() {
        messagePartDirectory.deleteRecursively()
    }

    @Test
    fun `save message with text_plain body`() {
        val messageData = buildMessage {
            header("Subject", "Test Message")
            header("From", "alice@domain.example")
            header("To", "Bob <bob@domain.example>")
            header("Cc", "<cloe@domain.example>")
            header("Date", "Mon, 12 Apr 2021 03:42:00 +0200")
            header("Message-ID", "<msg0001@domain.example>")

            textBody("Text")
        }.apply {
            setFlag(Flag.FLAGGED, true)
            setFlag(Flag.SEEN, true)
            setFlag(Flag.ANSWERED, true)
            setFlag(Flag.FORWARDED, true)
        }.toSaveMessageData(
            previewResult = PreviewResult.text("Preview"),
        )

        saveMessageOperations.saveRemoteMessage(folderId = 1, messageServerId = "uid1", messageData)

        val messages = sqliteDatabase.readMessages()
        assertThat(messages).hasSize(1)
        val message = messages.first()
        with(message) {
            assertThat(deleted).isEqualTo(0)
            assertThat(folderId).isEqualTo(1)
            assertThat(uid).isEqualTo("uid1")
            assertThat(subject).isEqualTo("Test Message")
            assertThat(date).isEqualTo(1618191720000L)
            assertThat(internalDate).isEqualTo(1618191720000L)
            assertThat(flags).isEqualTo("X_DOWNLOADED_FULL")
            assertThat(senderList).isEqualTo("alice@domain.example")
            assertThat(toList).isEqualTo(Address.pack(Address.parse("Bob <bob@domain.example>")))
            assertThat(ccList).isEqualTo("cloe@domain.example")
            assertThat(bccList).isEqualTo("")
            assertThat(replyToList).isEqualTo("")
            assertThat(attachmentCount).isEqualTo(0)
            assertThat(messageId).isEqualTo("<msg0001@domain.example>")
            assertThat(previewType).isEqualTo("text")
            assertThat(preview).isEqualTo("Preview")
            assertThat(mimeType).isEqualTo("text/plain")
            assertThat(empty).isEqualTo(0)
            assertThat(read).isEqualTo(1)
            assertThat(flagged).isEqualTo(1)
            assertThat(answered).isEqualTo(1)
            assertThat(forwarded).isEqualTo(1)
            assertThat(encryptionType).isNull()
        }

        val messageParts = sqliteDatabase.readMessageParts()
        assertThat(messageParts).hasSize(1)
        val messagePart = messageParts.first()
        with(messagePart) {
            assertThat(type).isEqualTo(MessagePartType.UNKNOWN)
            assertThat(root).isEqualTo(messagePart.id)
            assertThat(parent).isEqualTo(-1)
            assertThat(seq).isEqualTo(0)
            assertThat(mimeType).isEqualTo("text/plain")
            assertThat(displayName).isEqualTo("noname.txt")
            assertThat(header?.toString(Charsets.UTF_8)).isEqualTo(messageData.message.header())
            assertThat(encoding).isEqualTo("quoted-printable")
            assertThat(charset).isNull()
            assertThat(dataLocation).isEqualTo(DataLocation.IN_DATABASE)
            assertThat(decodedBodySize).isEqualTo(4)
            assertThat(data?.toString(Charsets.UTF_8)).isEqualTo("Text")
            assertThat(preamble).isNull()
            assertThat(epilogue).isNull()
            assertThat(boundary).isNull()
            assertThat(contentId).isNull()
            assertThat(serverExtra).isNull()
        }

        val threads = sqliteDatabase.readThreads()
        assertThat(threads).hasSize(1)
        val thread = threads.first()
        with(thread) {
            assertThat(messageId).isEqualTo(message.id)
            assertThat(root).isEqualTo(id)
            assertThat(parent).isNull()
        }
    }

    @Test
    fun `save message with multipart body`() {
        val messageData = buildMessage {
            multipart("alternative") {
                bodyPart("text/plain") {
                    textBody("plain")
                }
                bodyPart("text/html") {
                    textBody("html")
                }
            }
        }.toSaveMessageData()

        saveMessageOperations.saveRemoteMessage(folderId = 1, messageServerId = "uid1", messageData)

        val messages = sqliteDatabase.readMessages()
        assertThat(messages).hasSize(1)

        val messageParts = sqliteDatabase.readMessageParts()
        assertThat(messageParts).hasSize(3)

        val rootMessagePart = messageParts.first { it.seq == 0 }
        with(rootMessagePart) {
            assertThat(type).isEqualTo(MessagePartType.UNKNOWN)
            assertThat(root).isEqualTo(id)
            assertThat(parent).isEqualTo(-1)
            assertThat(mimeType).isEqualTo("multipart/alternative")
            assertThat(displayName).isNull()
            assertThat(header?.toString(Charsets.UTF_8)).isEqualTo(messageData.message.header())
            assertThat(encoding).isNull()
            assertThat(charset).isNull()
            assertThat(dataLocation).isEqualTo(DataLocation.CHILD_PART_CONTAINS_DATA)
            assertThat(decodedBodySize).isNull()
            assertThat(data).isNull()
            assertThat(preamble).isNull()
            assertThat(epilogue).isNull()
            assertThat(boundary).isEqualTo(messageData.message.boundary())
            assertThat(contentId).isNull()
            assertThat(serverExtra).isNull()
        }

        val textPlainMessagePart = messageParts.first { it.seq == 1 }
        with(textPlainMessagePart) {
            assertThat(type).isEqualTo(MessagePartType.UNKNOWN)
            assertThat(root).isEqualTo(rootMessagePart.id)
            assertThat(parent).isEqualTo(rootMessagePart.id)
            assertThat(mimeType).isEqualTo("text/plain")
            assertThat(displayName).isEqualTo("noname.txt")
            assertThat(header).isNotNull()
            assertThat(encoding).isEqualTo("quoted-printable")
            assertThat(charset).isNull()
            assertThat(dataLocation).isEqualTo(DataLocation.IN_DATABASE)
            assertThat(decodedBodySize).isEqualTo(5)
            assertThat(data?.toString(Charsets.UTF_8)).isEqualTo("plain")
            assertThat(preamble).isNull()
            assertThat(epilogue).isNull()
            assertThat(boundary).isNull()
            assertThat(contentId).isNull()
            assertThat(serverExtra).isNull()
        }

        val textHtmlMessagePart = messageParts.first { it.seq == 2 }
        with(textHtmlMessagePart) {
            assertThat(type).isEqualTo(MessagePartType.UNKNOWN)
            assertThat(root).isEqualTo(rootMessagePart.id)
            assertThat(parent).isEqualTo(rootMessagePart.id)
            assertThat(mimeType).isEqualTo("text/html")
            assertThat(displayName).isEqualTo("noname.html")
            assertThat(header).isNotNull()
            assertThat(encoding).isEqualTo("quoted-printable")
            assertThat(charset).isNull()
            assertThat(dataLocation).isEqualTo(DataLocation.IN_DATABASE)
            assertThat(decodedBodySize).isEqualTo(4)
            assertThat(data?.toString(Charsets.UTF_8)).isEqualTo("html")
            assertThat(preamble).isNull()
            assertThat(epilogue).isNull()
            assertThat(boundary).isNull()
            assertThat(contentId).isNull()
            assertThat(serverExtra).isNull()
        }
    }

    @Test
    fun `save message into existing thread`() {
        val messageId1 = sqliteDatabase.createMessage(
            folderId = 1,
            empty = true,
            messageIdHeader = "<msg0001@domain.example>",
        )
        val messageId2 = sqliteDatabase.createMessage(
            folderId = 1,
            empty = true,
            messageIdHeader = "<msg0002@domain.example>",
        )
        val messageId3 = sqliteDatabase.createMessage(
            folderId = 1,
            empty = false,
            messageIdHeader = "<msg0003@domain.example>",
        )
        val threadId1 = sqliteDatabase.createThread(messageId1)
        val threadId2 = sqliteDatabase.createThread(messageId2, root = threadId1, parent = threadId1)
        val threadId3 = sqliteDatabase.createThread(messageId3, root = threadId1, parent = threadId2)
        val messageData = buildMessage {
            header("Message-ID", "<msg0002@domain.example>")
            header("In-Reply-To", "<msg0001@domain.example>")

            textBody()
        }.toSaveMessageData()

        saveMessageOperations.saveRemoteMessage(folderId = 1, messageServerId = "uid1", messageData)

        val threads = sqliteDatabase.readThreads()
        assertThat(threads).hasSize(3)

        assertThat(threads.first { it.id == threadId1 }).isEqualTo(
            ThreadEntry(
                id = threadId1,
                messageId = messageId1,
                root = threadId1,
                parent = null,
            ),
        )

        assertThat(threads.first { it.id == threadId2 }).isEqualTo(
            ThreadEntry(
                id = threadId2,
                messageId = messageId2,
                root = threadId1,
                parent = threadId1,
            ),
        )

        assertThat(threads.first { it.id == threadId3 }).isEqualTo(
            ThreadEntry(
                id = threadId3,
                messageId = messageId3,
                root = threadId1,
                parent = threadId2,
            ),
        )
    }

    @Test
    fun `save message with references header should create empty messages`() {
        val messageData = buildMessage {
            header("Message-ID", "<msg0003@domain.example>")
            header("In-Reply-To", "<msg0002@domain.example>")
            header("References", "<msg0001@domain.example> <msg0002@domain.example>")

            textBody()
        }.toSaveMessageData()

        saveMessageOperations.saveRemoteMessage(folderId = 1, messageServerId = "uid1", messageData)

        val messages = sqliteDatabase.readMessages()
        assertThat(messages).hasSize(3)

        val threads = sqliteDatabase.readThreads()
        assertThat(threads).hasSize(3)

        val thread1 = threads.first { it.id == it.root }
        val message1 = messages.first { it.id == thread1.messageId }
        assertThat(message1.empty).isEqualTo(1)

        val thread2 = threads.first { it.parent == thread1.id }
        val message2 = messages.first { it.id == thread2.messageId }
        assertThat(message2.empty).isEqualTo(1)

        val thread3 = threads.first { it.parent == thread2.id }
        val message3 = messages.first { it.id == thread3.messageId }
        assertThat(message3.empty).isEqualTo(0)
        assertThat(message3.uid).isEqualTo("uid1")
    }

    @Test
    fun `save message with server ID already existing in MessageStore should replace that message`() {
        val existingMessageData = buildMessage {
            multipart("alternative") {
                bodyPart("text/plain") {
                    textBody("plain")
                }
                bodyPart("text/html") {
                    textBody("html")
                }
            }
        }.toSaveMessageData()
        saveMessageOperations.saveRemoteMessage(folderId = 1, messageServerId = "uid1", existingMessageData)
        val messageData = buildMessage {
            textBody("new")
        }.toSaveMessageData()

        saveMessageOperations.saveRemoteMessage(folderId = 1, messageServerId = "uid1", messageData)

        val messages = sqliteDatabase.readMessages()
        assertThat(messages).hasSize(1)

        val messageParts = sqliteDatabase.readMessageParts()
        assertThat(messageParts).hasSize(1)

        val messagePart = messageParts.first()
        with(messagePart) {
            assertThat(type).isEqualTo(MessagePartType.UNKNOWN)
            assertThat(root).isEqualTo(messagePart.id)
            assertThat(parent).isEqualTo(-1)
            assertThat(mimeType).isEqualTo("text/plain")
            assertThat(displayName).isEqualTo("noname.txt")
            assertThat(header).isNotNull()
            assertThat(encoding).isEqualTo("quoted-printable")
            assertThat(charset).isNull()
            assertThat(dataLocation).isEqualTo(DataLocation.IN_DATABASE)
            assertThat(decodedBodySize).isEqualTo(3)
            assertThat(data?.toString(Charsets.UTF_8)).isEqualTo("new")
            assertThat(preamble).isNull()
            assertThat(epilogue).isNull()
            assertThat(boundary).isNull()
            assertThat(contentId).isNull()
            assertThat(serverExtra).isNull()
        }

        val threads = sqliteDatabase.readThreads()
        assertThat(threads).hasSize(1)

        val thread = threads.first()
        val message = messages.first()
        assertThat(thread.root).isEqualTo(thread.id)
        assertThat(thread.parent).isNull()
        assertThat(thread.messageId).isEqualTo(message.id)
    }

    @Test
    fun `save local message`() {
        val messageData = buildMessage {
            textBody("local")
        }.toSaveMessageData(
            subject = "Provided subject",
            date = 1618191720000L,
            internalDate = 1618191720000L,
            previewResult = PreviewResult.text("Preview"),
        )

        val newMessageId = saveMessageOperations.saveLocalMessage(folderId = 1, messageData, existingMessageId = null)

        val messages = sqliteDatabase.readMessages()
        assertThat(messages).hasSize(1)

        val message = messages.first()
        with(message) {
            assertThat(id).isEqualTo(newMessageId)
            assertThat(deleted).isEqualTo(0)
            assertThat(folderId).isEqualTo(1)
            assertThat(uid).isNotNull().startsWith(K9.LOCAL_UID_PREFIX)
            assertThat(subject).isEqualTo("Provided subject")
            assertThat(date).isEqualTo(1618191720000L)
            assertThat(internalDate).isEqualTo(1618191720000L)
            assertThat(flags).isEqualTo("X_DOWNLOADED_FULL")
            assertThat(senderList).isEqualTo("")
            assertThat(toList).isEqualTo("")
            assertThat(ccList).isEqualTo("")
            assertThat(bccList).isEqualTo("")
            assertThat(replyToList).isEqualTo("")
            assertThat(attachmentCount).isEqualTo(0)
            assertThat(messageId).isNull()
            assertThat(previewType).isEqualTo("text")
            assertThat(preview).isEqualTo("Preview")
            assertThat(mimeType).isEqualTo("text/plain")
            assertThat(empty).isEqualTo(0)
            assertThat(read).isEqualTo(0)
            assertThat(flagged).isEqualTo(0)
            assertThat(answered).isEqualTo(0)
            assertThat(forwarded).isEqualTo(0)
            assertThat(encryptionType).isNull()
        }

        val messageParts = sqliteDatabase.readMessageParts()
        assertThat(messageParts).hasSize(1)

        val messagePart = messageParts.first()
        with(messagePart) {
            assertThat(type).isEqualTo(MessagePartType.UNKNOWN)
            assertThat(root).isEqualTo(messagePart.id)
            assertThat(parent).isEqualTo(-1)
            assertThat(mimeType).isEqualTo("text/plain")
            assertThat(displayName).isEqualTo("noname.txt")
            assertThat(header).isNotNull()
            assertThat(encoding).isEqualTo("quoted-printable")
            assertThat(charset).isNull()
            assertThat(dataLocation).isEqualTo(DataLocation.IN_DATABASE)
            assertThat(decodedBodySize).isEqualTo(5)
            assertThat(data?.toString(Charsets.UTF_8)).isEqualTo("local")
            assertThat(preamble).isNull()
            assertThat(epilogue).isNull()
            assertThat(boundary).isNull()
            assertThat(contentId).isNull()
            assertThat(serverExtra).isNull()
        }

        val threads = sqliteDatabase.readThreads()
        assertThat(threads).hasSize(1)

        val thread = threads.first()
        assertThat(thread.root).isEqualTo(thread.id)
        assertThat(thread.parent).isNull()
        assertThat(thread.messageId).isEqualTo(message.id)
    }

    @Test
    fun `replace local message`() {
        val existingMessageData = buildMessage {
            multipart("alternative") {
                bodyPart("text/plain") {
                    textBody("plain")
                }
                bodyPart("text/html") {
                    textBody("html")
                }
            }
        }.toSaveMessageData()
        val existingMessageId = saveMessageOperations.saveLocalMessage(
            folderId = 1,
            existingMessageData,
            existingMessageId = null,
        )
        val messageData = buildMessage {
            textBody("new")
        }.toSaveMessageData()

        val newMessageId = saveMessageOperations.saveLocalMessage(folderId = 1, messageData, existingMessageId)

        val messages = sqliteDatabase.readMessages()
        assertThat(messages).hasSize(1)

        assertThat(messages.first().id).isEqualTo(newMessageId)

        val messageParts = sqliteDatabase.readMessageParts()
        assertThat(messageParts).hasSize(1)

        val messagePart = messageParts.first()
        with(messagePart) {
            assertThat(type).isEqualTo(MessagePartType.UNKNOWN)
            assertThat(root).isEqualTo(messagePart.id)
            assertThat(parent).isEqualTo(-1)
            assertThat(mimeType).isEqualTo("text/plain")
            assertThat(displayName).isEqualTo("noname.txt")
            assertThat(header).isNotNull()
            assertThat(encoding).isEqualTo("quoted-printable")
            assertThat(charset).isNull()
            assertThat(dataLocation).isEqualTo(DataLocation.IN_DATABASE)
            assertThat(decodedBodySize).isEqualTo(3)
            assertThat(data?.toString(Charsets.UTF_8)).isEqualTo("new")
            assertThat(preamble).isNull()
            assertThat(epilogue).isNull()
            assertThat(boundary).isNull()
            assertThat(contentId).isNull()
            assertThat(serverExtra).isNull()
        }

        val threads = sqliteDatabase.readThreads()
        assertThat(threads).hasSize(1)

        val thread = threads.first()
        val message = messages.first()
        assertThat(thread.root).isEqualTo(thread.id)
        assertThat(thread.parent).isNull()
        assertThat(thread.messageId).isEqualTo(message.id)
    }

    private fun Message.toSaveMessageData(
        subject: String? = getSubject(),
        date: Long = sentDate?.time ?: System.currentTimeMillis(),
        internalDate: Long = date,
        downloadState: MessageDownloadState = getDownloadState(),
        attachmentCount: Int = 0,
        previewResult: PreviewResult = PreviewResult.none(),
        textForSearchIndex: String? = null,
        encryptionType: String? = null,
    ): SaveMessageData {
        return SaveMessageData(
            message = this,
            subject,
            date,
            internalDate,
            downloadState,
            attachmentCount,
            previewResult,
            textForSearchIndex,
            encryptionType,
        )
    }

    private fun Message.getDownloadState(): MessageDownloadState {
        if (body == null) return MessageDownloadState.ENVELOPE

        val stack = Stack<Part>()
        stack.push(this)

        while (stack.isNotEmpty()) {
            val part = stack.pop()
            when (val body = part.body) {
                null -> return MessageDownloadState.PARTIAL
                is Multipart -> {
                    for (i in 0 until body.count) {
                        stack.push(body.getBodyPart(i))
                    }
                }
            }
        }

        return MessageDownloadState.FULL
    }

    private fun Message.header(): String {
        val outputStream = ByteArrayOutputStream()
        writeHeaderTo(outputStream)
        return outputStream.toString("UTF-8")
    }

    private fun Message.boundary(): String? = (body as Multipart).boundary
}
