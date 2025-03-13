package com.fsck.k9.mail.store.pop3

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isSameInstanceAs
import com.fsck.k9.mail.AuthenticationFailedException
import com.fsck.k9.mail.Body
import com.fsck.k9.mail.FetchProfile
import com.fsck.k9.mail.MessageRetrievalListener
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.internet.BinaryTempFileBody
import com.fsck.k9.mail.store.pop3.Pop3Commands.STAT_COMMAND
import com.fsck.k9.mail.testing.crlf
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.stubbing

class Pop3FolderTest {
    private val connection = mock<Pop3Connection> {
        on { executeSimpleCommand(STAT_COMMAND) } doReturn "+OK $MESSAGE_COUNT 0"
        on { isOpen } doReturn true
    }
    private val store = mock<Pop3Store> {
        on { createConnection() } doReturn connection
    }
    private val messageRetrievalListener = mock<MessageRetrievalListener<Pop3Message>>()
    private val folder = Pop3Folder(store, Pop3Folder.INBOX)

    @Before
    fun setUp() {
        BinaryTempFileBody.setTempDirectory(File(System.getProperty("java.io.tmpdir")))
    }

    @Test(expected = MessagingException::class)
    fun `open() without Inbox folder should throw`() {
        val folder = Pop3Folder(store, "TestFolder")

        folder.open()
    }

    @Test
    fun `open() without Inbox folder should not call Pop3Store_createConnection()`() {
        val folder = Pop3Folder(store, "TestFolder")

        try {
            folder.open()
        } catch (ignored: Exception) {
        }

        verify(store, never()).createConnection()
    }

    @Test(expected = MessagingException::class)
    fun `open() with exception when creating a connection should throw`() {
        stubbing(store) {
            on { createConnection() } doThrow MessagingException("Test")
        }

        folder.open()
    }

    @Test(expected = AuthenticationFailedException::class)
    fun `open() with failed authentication should throw`() {
        stubbing(connection) {
            on { open() } doThrow AuthenticationFailedException("Test")
        }

        folder.open()
    }

    @Test
    fun `open() should set message count from STAT response`() {
        folder.open()

        assertThat(folder.messageCount).isEqualTo(MESSAGE_COUNT)
    }

    @Test(expected = MessagingException::class)
    fun `open() with STAT command failing should throw`() {
        stubbing(connection) {
            on { executeSimpleCommand(STAT_COMMAND) } doThrow Pop3ErrorResponse("Test")
        }

        folder.open()
    }

    @Test
    fun `open() should open connection`() {
        folder.open()

        verify(store, times(1)).createConnection()
        verify(connection).open()
    }

    @Test
    fun `open() with connection already open should not create another connection`() {
        folder.open()

        folder.open()

        verify(store, times(1)).createConnection()
    }

    @Test
    fun `close() with closed folder should not throw`() {
        folder.close()
    }

    @Test
    fun `close() with open folder should not throw`() {
        folder.open()

        folder.close()
    }

    @Test
    fun `close() with open folder should send QUIT command`() {
        folder.open()

        folder.close()

        verify(connection).executeSimpleCommand(Pop3Commands.QUIT_COMMAND)
    }

    @Test
    fun `close() with exception when sending QUIT command should not throw`() {
        stubbing(connection) {
            on { executeSimpleCommand(Pop3Commands.QUIT_COMMAND) } doThrow Pop3ErrorResponse("Test")
        }
        folder.open()

        folder.close()
    }

    @Test
    fun `close() with open folder should close connection`() {
        folder.open()

        folder.close()

        verify(connection).close()
    }

    @Test
    fun `getMessages() should return list of messages on server`() {
        stubbing(connection) {
            on { readLine() } doReturn "1 $MESSAGE_SERVER_ID" doReturn "."
        }
        folder.open()

        val result = folder.getMessages(1, 1, messageRetrievalListener)

        assertThat(result).hasSize(1)
    }

    @Test(expected = MessagingException::class)
    fun `getMessages() with invalid set should throw`() {
        folder.open()

        folder.getMessages(2, 1, messageRetrievalListener)
    }

    @Test(expected = MessagingException::class)
    fun `getMessages() with IOException when reading line should throw`() {
        stubbing(connection) {
            on { readLine() } doThrow IOException("Test")
        }
        folder.open()

        folder.getMessages(1, 1, messageRetrievalListener)
    }

    @Test
    fun `getMessage() with previously fetched message should return message`() {
        folder.open()
        val messageList = setupMessageFromServer()

        val message = folder.getMessage(MESSAGE_SERVER_ID)

        assertThat(message).isSameInstanceAs(messageList.first())
    }

    @Test
    fun `getMessage() without previously fetched message should return new message`() {
        folder.open()

        val message = folder.getMessage(MESSAGE_SERVER_ID)

        assertThat(message).isNotNull()
    }

    @Test
    fun `fetch() with ENVELOPE profile should set size of message`() {
        folder.open()
        val messageList = setupMessageFromServer()
        val fetchProfile = FetchProfile()
        fetchProfile.add(FetchProfile.Item.ENVELOPE)
        stubbing(connection) {
            on { readLine() } doReturn "1 100" doReturn "."
        }

        folder.fetch(messageList, fetchProfile, messageRetrievalListener, MAX_DOWNLOAD_SIZE)

        assertThat(messageList.first().size).isEqualTo(100)
    }

    @Test
    fun `fetch() with BODY profile should set content of message`() {
        val messageInputStream =
            """
            From: <adam@example.org>
            To: <eva@example.org>
            Subject: Testmail
            MIME-Version: 1.0
            Content-type: text/plain
            Content-Transfer-Encoding: 7bit

            this is some test text.
            """.trimIndent().crlf().byteInputStream()
        folder.open()
        val messageList = setupMessageFromServer()
        val fetchProfile = FetchProfile()
        fetchProfile.add(FetchProfile.Item.BODY)
        stubbing(connection) {
            on { readLine() } doReturn "1 100" doReturn "."
            on { inputStream } doReturn messageInputStream
        }

        folder.fetch(messageList, fetchProfile, messageRetrievalListener, MAX_DOWNLOAD_SIZE)

        assertThat(messageList.first().body.writeToString()).isEqualTo("this is some test text.")
    }

    private fun setupMessageFromServer(): List<Pop3Message> {
        stubbing(connection) {
            on { readLine() } doReturn "1 $MESSAGE_SERVER_ID" doReturn "."
        }

        return folder.getMessages(1, 1, messageRetrievalListener)
    }

    private fun Body.writeToString(): String {
        return ByteArrayOutputStream().also { outputStream ->
            writeTo(outputStream)
        }.toByteArray().decodeToString()
    }

    companion object {
        private const val MESSAGE_COUNT = 10
        private const val MESSAGE_SERVER_ID = "abcd"
        private const val MAX_DOWNLOAD_SIZE = -1
    }
}
