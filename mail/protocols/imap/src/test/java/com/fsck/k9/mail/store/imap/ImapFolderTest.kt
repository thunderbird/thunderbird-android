package com.fsck.k9.mail.store.imap

import com.fsck.k9.mail.Body
import com.fsck.k9.mail.DefaultBodyFactory
import com.fsck.k9.mail.FetchProfile
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.K9LibRobolectricTestRunner
import com.fsck.k9.mail.MessageRetrievalListener
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.Part
import com.fsck.k9.mail.internet.BinaryTempFileBody
import com.fsck.k9.mail.internet.MimeHeader
import com.fsck.k9.mail.store.imap.ImapResponseHelper.createImapResponse
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import java.io.IOException
import java.util.Date
import java.util.TimeZone
import okio.Buffer
import org.apache.james.mime4j.util.MimeUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anySet
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.ArgumentMatchers.startsWith
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.robolectric.RuntimeEnvironment

@RunWith(K9LibRobolectricTestRunner::class)
class ImapFolderTest {
    private val imapStore = mock<ImapStore> {
        on { combinedPrefix } doReturn ""
        on { logLabel } doReturn "Account"
    }
    private val imapConnection = mock<ImapConnection>()

    @Before
    fun setUp() {
        BinaryTempFileBody.setTempDirectory(RuntimeEnvironment.application.cacheDir)
    }

    @Test
    fun open_readWrite_shouldOpenFolder() {
        val imapFolder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RW)

        imapFolder.open(ImapFolder.OPEN_MODE_RW)

        assertTrue(imapFolder.isOpen)
    }

    @Test
    fun open_readOnly_shouldOpenFolder() {
        val imapFolder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RO)

        imapFolder.open(ImapFolder.OPEN_MODE_RO)

        assertTrue(imapFolder.isOpen)
    }

    @Test
    fun open_shouldFetchMessageCount() {
        val imapFolder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RW)

        imapFolder.open(ImapFolder.OPEN_MODE_RW)

        assertEquals(23, imapFolder.messageCount)
    }

    @Test
    fun open_readWrite_shouldMakeGetModeReturnReadWrite() {
        val imapFolder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RW)

        imapFolder.open(ImapFolder.OPEN_MODE_RW)

        assertEquals(ImapFolder.OPEN_MODE_RW, imapFolder.mode)
    }

    @Test
    fun open_readOnly_shouldMakeGetModeReturnReadOnly() {
        val imapFolder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RO)

        imapFolder.open(ImapFolder.OPEN_MODE_RO)

        assertEquals(ImapFolder.OPEN_MODE_RO, imapFolder.mode)
    }

    @Test
    fun open_shouldMakeExistReturnTrueWithoutExecutingAdditionalCommands() {
        val imapFolder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RW)

        imapFolder.open(ImapFolder.OPEN_MODE_RW)

        assertTrue(imapFolder.exists())
        verify(imapConnection, times(1)).executeSimpleCommand(anyString())
    }

    @Test
    fun open_calledTwice_shouldReuseSameImapConnection() {
        val imapFolder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RW)
        imapFolder.open(ImapFolder.OPEN_MODE_RW)

        imapFolder.open(ImapFolder.OPEN_MODE_RW)

        verify(imapStore, times(1)).connection
    }

    @Test
    fun open_withConnectionThrowingOnReUse_shouldCreateNewImapConnection() {
        val imapFolder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RW)
        imapFolder.open(ImapFolder.OPEN_MODE_RW)

        doThrow(IOException::class).whenever(imapConnection).executeSimpleCommand(Commands.NOOP)
        imapFolder.open(ImapFolder.OPEN_MODE_RW)

        verify(imapStore, times(2)).connection
    }

    @Test
    fun open_withIoException_shouldThrowMessagingException() {
        val imapFolder = createFolder("Folder")
        whenever(imapStore.connection).thenReturn(imapConnection)
        doThrow(IOException::class).whenever(imapConnection).executeSimpleCommand("SELECT \"Folder\"")

        try {
            imapFolder.open(ImapFolder.OPEN_MODE_RW)
            fail("Expected exception")
        } catch (e: MessagingException) {
            assertNotNull(e.cause)
            assertEquals(IOException::class.java, e.cause!!.javaClass)
        }
    }

    @Test
    fun open_withMessagingException_shouldThrowMessagingException() {
        val imapFolder = createFolder("Folder")
        whenever(imapStore.connection).thenReturn(imapConnection)
        doThrow(MessagingException::class).whenever(imapConnection).executeSimpleCommand("SELECT \"Folder\"")

        try {
            imapFolder.open(ImapFolder.OPEN_MODE_RW)
            fail("Expected exception")
        } catch (ignored: MessagingException) {
        }
    }

    @Test
    fun open_withoutExistsResponse_shouldThrowMessagingException() {
        val imapFolder = createFolder("Folder")
        whenever(imapStore.connection).thenReturn(imapConnection)
        val selectResponses = listOf(
            createImapResponse("* OK [UIDNEXT 57576] Predicted next UID"),
            createImapResponse("2 OK [READ-WRITE] Select completed.")
        )
        whenever(imapConnection.executeSimpleCommand("SELECT \"Folder\"")).thenReturn(selectResponses)

        try {
            imapFolder.open(ImapFolder.OPEN_MODE_RW)
            fail("Expected exception")
        } catch (e: MessagingException) {
            assertEquals("Did not find message count during open", e.message)
        }
    }

    @Test
    fun close_shouldCloseImapFolder() {
        val imapFolder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RW)
        imapFolder.open(ImapFolder.OPEN_MODE_RW)

        imapFolder.close()

        assertFalse(imapFolder.isOpen)
    }

    @Test
    fun exists_withClosedFolder_shouldOpenConnectionAndIssueStatusCommand() {
        val imapFolder = createFolder("Folder")
        whenever(imapStore.connection).thenReturn(imapConnection)

        imapFolder.exists()

        verify(imapConnection).executeSimpleCommand("STATUS \"Folder\" (UIDVALIDITY)")
    }

    @Test
    fun exists_withoutNegativeImapResponse_shouldReturnTrue() {
        val imapFolder = createFolder("Folder")
        whenever(imapStore.connection).thenReturn(imapConnection)

        val folderExists = imapFolder.exists()

        assertTrue(folderExists)
    }

    @Test
    fun exists_withNegativeImapResponse_shouldReturnFalse() {
        val imapFolder = createFolder("Folder")
        whenever(imapStore.connection).thenReturn(imapConnection)
        doThrow(NegativeImapResponseException::class)
            .whenever(imapConnection).executeSimpleCommand("STATUS \"Folder\" (UIDVALIDITY)")

        val folderExists = imapFolder.exists()

        assertFalse(folderExists)
    }

    @Test
    fun create_withClosedFolder_shouldOpenConnectionAndIssueCreateCommand() {
        val imapFolder = createFolder("Folder")
        whenever(imapStore.connection).thenReturn(imapConnection)

        imapFolder.create()

        verify(imapConnection).executeSimpleCommand("CREATE \"Folder\"")
    }

    @Test
    fun create_withoutNegativeImapResponse_shouldReturnTrue() {
        val imapFolder = createFolder("Folder")
        whenever(imapStore.connection).thenReturn(imapConnection)

        val success = imapFolder.create()

        assertTrue(success)
    }

    @Test
    fun create_withNegativeImapResponse_shouldReturnFalse() {
        val imapFolder = createFolder("Folder")
        whenever(imapStore.connection).thenReturn(imapConnection)
        doThrow(NegativeImapResponseException::class).whenever(imapConnection).executeSimpleCommand("CREATE \"Folder\"")

        val success = imapFolder.create()

        assertFalse(success)
    }

    @Test
    fun copyMessages_withEmptyMessageList_shouldReturnNull() {
        val sourceFolder = createFolder("Source")
        val destinationFolder = createFolder("Destination")
        val messages = emptyList<ImapMessage>()

        val uidMapping = sourceFolder.copyMessages(messages, destinationFolder)

        assertNull(uidMapping)
    }

    @Test
    fun copyMessages_withClosedFolder_shouldThrow() {
        val sourceFolder = createFolder("Source")
        val destinationFolder = createFolder("Destination")
        whenever(imapStore.connection).thenReturn(imapConnection)
        whenever(imapStore.combinedPrefix).thenReturn("")
        val messages = listOf(mock<ImapMessage>())

        try {
            sourceFolder.copyMessages(messages, destinationFolder)
            fail("Expected exception")
        } catch (e: MessagingException) {
            assertEquals("Folder Source is not open.", e.message)
        }
    }

    @Test
    fun copyMessages() {
        val sourceFolder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RW)
        val destinationFolder = createFolder("Destination")
        val messages = listOf(createImapMessage("1"))
        setupCopyResponse("x OK [COPYUID 23 1 101] Success")
        sourceFolder.open(ImapFolder.OPEN_MODE_RW)

        val uidMapping = sourceFolder.copyMessages(messages, destinationFolder)

        assertNotNull(uidMapping)
        assertEquals("101", uidMapping!!["1"])
    }

    @Test
    fun moveMessages_shouldCopyMessages() {
        val sourceFolder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RW)
        val destinationFolder = createFolder("Destination")
        val messages = listOf(createImapMessage("1"))
        setupCopyResponse("x OK [COPYUID 23 1 101] Success")
        sourceFolder.open(ImapFolder.OPEN_MODE_RW)

        val uidMapping = sourceFolder.moveMessages(messages, destinationFolder)

        assertNotNull(uidMapping)
        assertEquals("101", uidMapping!!["1"])
    }

    @Test
    fun moveMessages_shouldDeleteMessagesFromSourceFolder() {
        val sourceFolder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RW)
        val destinationFolder = createFolder("Destination")
        val messages = listOf(createImapMessage("1"))
        sourceFolder.open(ImapFolder.OPEN_MODE_RW)

        sourceFolder.moveMessages(messages, destinationFolder)

        assertCommandWithIdsIssued("UID STORE 1 +FLAGS.SILENT (\\Deleted)")
    }

    @Test
    fun moveMessages_withEmptyMessageList_shouldReturnNull() {
        val sourceFolder = createFolder("Source")
        val destinationFolder = createFolder("Destination")
        val messages = emptyList<ImapMessage>()

        val uidMapping = sourceFolder.moveMessages(messages, destinationFolder)

        assertNull(uidMapping)
    }

    @Test
    fun getUnreadMessageCount_withClosedFolder_shouldThrow() {
        val folder = createFolder("Folder")
        whenever(imapStore.connection).thenReturn(imapConnection)
        try {
            folder.unreadMessageCount
            fail("Expected exception")
        } catch (e: MessagingException) {
            assertCheckOpenErrorMessage("Folder", e)
        }
    }

    @Test
    fun getUnreadMessageCount_connectionThrowsIOException_shouldThrowMessagingException() {
        val folder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RW)
        whenever(imapConnection.executeSimpleCommand("SEARCH 1:* UNSEEN NOT DELETED")).thenThrow(IOException())
        folder.open(ImapFolder.OPEN_MODE_RW)

        try {
            folder.unreadMessageCount
            fail("Expected exception")
        } catch (e: MessagingException) {
            assertEquals("IO Error", e.message)
        }
    }

    @Test
    fun getUnreadMessageCount() {
        val folder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RW)
        val imapResponses = listOf(createImapResponse("* SEARCH 1 2 3"))
        whenever(imapConnection.executeSimpleCommand("SEARCH 1:* UNSEEN NOT DELETED")).thenReturn(imapResponses)
        folder.open(ImapFolder.OPEN_MODE_RW)

        val unreadMessageCount = folder.unreadMessageCount

        assertEquals(3, unreadMessageCount)
    }

    @Test
    fun getFlaggedMessageCount_withClosedFolder_shouldThrow() {
        val folder = createFolder("Folder")
        whenever(imapStore.connection).thenReturn(imapConnection)

        try {
            folder.flaggedMessageCount
            fail("Expected exception")
        } catch (e: MessagingException) {
            assertCheckOpenErrorMessage("Folder", e)
        }
    }

    @Test
    fun getFlaggedMessageCount() {
        val folder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RW)
        val imapResponses = listOf(
            createImapResponse("* SEARCH 1 2"),
            createImapResponse("* SEARCH 23 42")
        )
        whenever(imapConnection.executeSimpleCommand("SEARCH 1:* FLAGGED NOT DELETED")).thenReturn(imapResponses)
        folder.open(ImapFolder.OPEN_MODE_RW)

        val flaggedMessageCount = folder.flaggedMessageCount

        assertEquals(4, flaggedMessageCount)
    }

    @Test
    fun getHighestUid() {
        val folder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RW)
        setupUidSearchResponses("* SEARCH 42")
        folder.open(ImapFolder.OPEN_MODE_RW)

        val highestUid = folder.highestUid

        assertEquals(42L, highestUid)
    }

    @Test
    fun getHighestUid_imapConnectionThrowsNegativesResponse_shouldReturnMinusOne() {
        val folder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RW)
        doThrow(NegativeImapResponseException::class).whenever(imapConnection).executeSimpleCommand("UID SEARCH *:*")
        folder.open(ImapFolder.OPEN_MODE_RW)

        val highestUid = folder.highestUid

        assertEquals(-1L, highestUid)
    }

    @Test
    fun getHighestUid_imapConnectionThrowsIOException_shouldThrowMessagingException() {
        val folder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RW)
        doThrow(IOException::class).whenever(imapConnection).executeSimpleCommand("UID SEARCH *:*")
        folder.open(ImapFolder.OPEN_MODE_RW)

        try {
            folder.highestUid
            fail("Expected MessagingException")
        } catch (e: MessagingException) {
            assertEquals("IO Error", e.message)
        }
    }

    @Test
    fun getMessages_withoutDateConstraint() {
        val folder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RW)
        setupUidSearchResponses("* SEARCH 3", "* SEARCH 5", "* SEARCH 6")
        folder.open(ImapFolder.OPEN_MODE_RW)

        val messages = folder.getMessages(1, 10, null, null)

        assertNotNull(messages)
        assertEquals(setOf("3", "5", "6"), extractMessageUids(messages))
    }

    @Test
    fun getMessages_withDateConstraint() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        val folder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RW)
        setupUidSearchResponses("* SEARCH 47", "* SEARCH 18")
        folder.open(ImapFolder.OPEN_MODE_RW)

        val messages = folder.getMessages(1, 10, Date(1454719826000L), null)

        assertNotNull(messages)
        assertEquals(setOf("18", "47"), extractMessageUids(messages))
    }

    @Test
    fun getMessages_withListener_shouldCallListener() {
        val folder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RW)
        setupUidSearchResponses("* SEARCH 99")
        folder.open(ImapFolder.OPEN_MODE_RW)
        val listener = createMessageRetrievalListener()

        val messages = folder.getMessages(1, 10, null, listener)

        verify(listener).messageStarted("99", 0, 1)
        verify(listener).messageFinished(messages[0], 0, 1)
        verifyNoMoreInteractions(listener)
    }

    @Test
    fun getMessages_withInvalidStartArgument_shouldThrow() {
        val folder = createFolder("Folder")

        try {
            folder.getMessages(0, 10, null, null)
            fail("Expected exception")
        } catch (e: MessagingException) {
            assertEquals("Invalid message set 0 10", e.message)
        }
    }

    @Test
    fun getMessages_withInvalidEndArgument_shouldThrow() {
        val folder = createFolder("Folder")

        try {
            folder.getMessages(10, 0, null, null)
            fail("Expected exception")
        } catch (e: MessagingException) {
            assertEquals("Invalid message set 10 0", e.message)
        }
    }

    @Test
    fun getMessages_withEndArgumentSmallerThanStartArgument_shouldThrow() {
        val folder = createFolder("Folder")

        try {
            folder.getMessages(10, 5, null, null)
            fail("Expected exception")
        } catch (e: MessagingException) {
            assertEquals("Invalid message set 10 5", e.message)
        }
    }

    @Test
    fun getMessages_withClosedFolder_shouldThrow() {
        val folder = createFolder("Folder")
        whenever(imapStore.connection).thenReturn(imapConnection)

        try {
            folder.getMessages(1, 5, null, null)
            fail("Expected exception")
        } catch (e: MessagingException) {
            assertCheckOpenErrorMessage("Folder", e)
        }
    }

    @Test
    fun getMessages_sequenceNumbers_withClosedFolder_shouldThrow() {
        val folder = createFolder("Folder")
        whenever(imapStore.connection).thenReturn(imapConnection)

        try {
            folder.getMessages(setOf(1L, 2L, 5L), false, null)
            fail("Expected exception")
        } catch (e: MessagingException) {
            assertCheckOpenErrorMessage("Folder", e)
        }
    }

    @Test
    fun getMessages_sequenceNumbers() {
        val folder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RW)
        setupUidSearchResponses("* SEARCH 17", "* SEARCH 18", "* SEARCH 49")
        folder.open(ImapFolder.OPEN_MODE_RW)

        val messages = folder.getMessages(setOf(1L, 2L, 5L), false, null)

        assertNotNull(messages)
        assertEquals(setOf("17", "18", "49"), extractMessageUids(messages))
    }

    @Test
    fun getMessages_sequenceNumbers_withListener_shouldCallListener() {
        val folder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RW)
        setupUidSearchResponses("* SEARCH 99")
        folder.open(ImapFolder.OPEN_MODE_RW)
        val listener = createMessageRetrievalListener()

        val messages = folder.getMessages(setOf(1L), true, listener)

        verify(listener).messageStarted("99", 0, 1)
        verify(listener).messageFinished(messages[0], 0, 1)
        verifyNoMoreInteractions(listener)
    }

    @Test
    fun getMessagesFromUids_withClosedFolder_shouldThrow() {
        val folder = createFolder("Folder")
        whenever(imapStore.connection).thenReturn(imapConnection)

        try {
            folder.getMessagesFromUids(listOf("11", "22", "25"))
            fail("Expected exception")
        } catch (e: MessagingException) {
            assertCheckOpenErrorMessage("Folder", e)
        }
    }

    @Test
    fun getMessagesFromUids() {
        val folder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RW)
        setupUidSearchResponses("* SEARCH 11", "* SEARCH 22", "* SEARCH 25")
        folder.open(ImapFolder.OPEN_MODE_RW)

        val messages = folder.getMessagesFromUids(listOf("11", "22", "25"))

        assertNotNull(messages)
        assertEquals(setOf("11", "22", "25"), extractMessageUids(messages))
    }

    @Test
    fun areMoreMessagesAvailable_withClosedFolder_shouldThrow() {
        val folder = createFolder("Folder")
        whenever(imapStore.connection).thenReturn(imapConnection)

        try {
            folder.areMoreMessagesAvailable(10, Date())
            fail("Expected exception")
        } catch (e: MessagingException) {
            assertCheckOpenErrorMessage("Folder", e)
        }
    }

    @Test
    fun areMoreMessagesAvailable_withAdditionalMessages_shouldReturnTrue() {
        val folder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RW)
        setupSearchResponses("* SEARCH 42")
        folder.open(ImapFolder.OPEN_MODE_RW)

        val areMoreMessagesAvailable = folder.areMoreMessagesAvailable(10, null)

        assertTrue(areMoreMessagesAvailable)
    }

    @Test
    fun areMoreMessagesAvailable_withoutAdditionalMessages_shouldReturnFalse() {
        val folder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RW)
        setupSearchResponses("1 OK SEARCH completed")
        folder.open(ImapFolder.OPEN_MODE_RW)

        val areMoreMessagesAvailable = folder.areMoreMessagesAvailable(600, null)

        assertFalse(areMoreMessagesAvailable)
    }

    @Test
    fun areMoreMessagesAvailable_withIndexOfOne_shouldReturnFalseWithoutPerformingSearch() {
        val folder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RW)
        folder.open(ImapFolder.OPEN_MODE_RW)

        val areMoreMessagesAvailable = folder.areMoreMessagesAvailable(1, null)

        assertFalse(areMoreMessagesAvailable)
        // SELECT during OPEN and no more
        verify(imapConnection, times(1)).executeSimpleCommand(anyString())
    }

    @Test
    fun areMoreMessagesAvailable_withoutAdditionalMessages_shouldIssueSearchCommandsUntilAllMessagesSearched() {
        val folder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RW)
        setupSearchResponses("1 OK SEARCH Completed")
        folder.open(ImapFolder.OPEN_MODE_RW)

        folder.areMoreMessagesAvailable(600, null)

        assertCommandIssued("SEARCH 100:599 NOT DELETED")
        assertCommandIssued("SEARCH 1:99 NOT DELETED")
    }

    @Test
    fun fetch_withNullMessageListArgument_shouldDoNothing() {
        val folder = createFolder("Folder")
        val fetchProfile = createFetchProfile()

        folder.fetch(null, fetchProfile, null, MAX_DOWNLOAD_SIZE)

        verifyNoMoreInteractions(imapStore)
    }

    @Test
    fun fetch_withEmptyMessageListArgument_shouldDoNothing() {
        val folder = createFolder("Folder")
        val fetchProfile = createFetchProfile()

        folder.fetch(emptyList(), fetchProfile, null, MAX_DOWNLOAD_SIZE)

        verifyNoMoreInteractions(imapStore)
    }

    @Test
    fun fetch_withFlagsFetchProfile_shouldIssueRespectiveCommand() {
        val folder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RO)
        folder.open(ImapFolder.OPEN_MODE_RO)
        whenever(imapConnection.readResponse(anyOrNull())).thenReturn(createImapResponse("x OK"))
        val messages = createImapMessages("1")
        val fetchProfile = createFetchProfile(FetchProfile.Item.FLAGS)

        folder.fetch(messages, fetchProfile, null, MAX_DOWNLOAD_SIZE)

        verify(imapConnection).sendCommand("UID FETCH 1 (UID FLAGS)", false)
    }

    @Test
    fun fetch_withEnvelopeFetchProfile_shouldIssueRespectiveCommand() {
        val folder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RO)
        folder.open(ImapFolder.OPEN_MODE_RO)
        whenever(imapConnection.readResponse(anyOrNull())).thenReturn(createImapResponse("x OK"))
        val messages = createImapMessages("1")
        val fetchProfile = createFetchProfile(FetchProfile.Item.ENVELOPE)

        folder.fetch(messages, fetchProfile, null, MAX_DOWNLOAD_SIZE)

        verify(imapConnection).sendCommand(
            "UID FETCH 1 (UID INTERNALDATE RFC822.SIZE BODY.PEEK[HEADER.FIELDS " +
                "(date subject from content-type to cc reply-to message-id references in-reply-to X-K9mail-Identity)])",
            false
        )
    }

    @Test
    fun fetch_withStructureFetchProfile_shouldIssueRespectiveCommand() {
        val folder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RO)
        folder.open(ImapFolder.OPEN_MODE_RO)
        whenever(imapConnection.readResponse(anyOrNull())).thenReturn(createImapResponse("x OK"))
        val messages = createImapMessages("1")
        val fetchProfile = createFetchProfile(FetchProfile.Item.STRUCTURE)

        folder.fetch(messages, fetchProfile, null, MAX_DOWNLOAD_SIZE)

        verify(imapConnection).sendCommand("UID FETCH 1 (UID BODYSTRUCTURE)", false)
    }

    @Test
    fun fetch_withStructureFetchProfile_shouldSetContentType() {
        val folder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RO)
        folder.open(ImapFolder.OPEN_MODE_RO)
        val bodyStructure = "(\"TEXT\" \"PLAIN\" (\"CHARSET\" \"US-ASCII\") NIL NIL \"7BIT\" 2279 48)"
        whenever(imapConnection.readResponse(anyOrNull()))
            .thenReturn(createImapResponse("* 1 FETCH (BODYSTRUCTURE $bodyStructure UID 1)"))
            .thenReturn(createImapResponse("x OK"))
        val messages = createImapMessages("1")
        val fetchProfile = createFetchProfile(FetchProfile.Item.STRUCTURE)

        folder.fetch(messages, fetchProfile, null, MAX_DOWNLOAD_SIZE)

        verify(messages[0]).setHeader(MimeHeader.HEADER_CONTENT_TYPE, "text/plain;\r\n CHARSET=\"US-ASCII\"")
    }

    @Test
    fun fetch_withBodySaneFetchProfile_shouldIssueRespectiveCommand() {
        val folder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RO)
        folder.open(ImapFolder.OPEN_MODE_RO)
        whenever(imapConnection.readResponse(anyOrNull())).thenReturn(createImapResponse("x OK"))
        val messages = createImapMessages("1")
        val fetchProfile = createFetchProfile(FetchProfile.Item.BODY_SANE)

        folder.fetch(messages, fetchProfile, null, 4096)

        verify(imapConnection).sendCommand("UID FETCH 1 (UID BODY.PEEK[]<0.4096>)", false)
    }

    @Test
    fun fetch_withBodySaneFetchProfileAndNoMaximumDownloadSize_shouldIssueRespectiveCommand() {
        val folder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RO)
        folder.open(ImapFolder.OPEN_MODE_RO)
        whenever(imapConnection.readResponse(anyOrNull())).thenReturn(createImapResponse("x OK"))
        val messages = createImapMessages("1")
        val fetchProfile = createFetchProfile(FetchProfile.Item.BODY_SANE)

        folder.fetch(messages, fetchProfile, null, 0)

        verify(imapConnection).sendCommand("UID FETCH 1 (UID BODY.PEEK[])", false)
    }

    @Test
    fun fetch_withBodyFetchProfileAndNoMaximumDownloadSize_shouldIssueRespectiveCommand() {
        val folder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RO)
        folder.open(ImapFolder.OPEN_MODE_RO)
        whenever(imapConnection.readResponse(anyOrNull())).thenReturn(createImapResponse("x OK"))
        val messages = createImapMessages("1")
        val fetchProfile = createFetchProfile(FetchProfile.Item.BODY)

        folder.fetch(messages, fetchProfile, null, MAX_DOWNLOAD_SIZE)

        verify(imapConnection).sendCommand("UID FETCH 1 (UID BODY.PEEK[])", false)
    }

    @Test
    fun fetch_withFlagsFetchProfile_shouldSetFlags() {
        val folder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RO)
        folder.open(ImapFolder.OPEN_MODE_RO)
        val messages = createImapMessages("1")
        val fetchProfile = createFetchProfile(FetchProfile.Item.FLAGS)
        whenever(imapConnection.readResponse(anyOrNull()))
            .thenReturn(createImapResponse("* 1 FETCH (FLAGS (\\Seen) UID 1)"))
            .thenReturn(createImapResponse("x OK"))

        folder.fetch(messages, fetchProfile, null, MAX_DOWNLOAD_SIZE)

        verify(messages[0]).setFlag(Flag.SEEN, true)
    }

    @Test
    fun fetchPart_withTextSection_shouldIssueRespectiveCommand() {
        val folder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RO)
        folder.open(ImapFolder.OPEN_MODE_RO)
        val message = createImapMessage("1")
        val part = createPart("TEXT")
        whenever(imapConnection.readResponse(anyOrNull())).thenReturn(createImapResponse("x OK"))

        folder.fetchPart(message, part, null, mock(), 4096)

        verify(imapConnection).sendCommand("UID FETCH 1 (UID BODY.PEEK[TEXT]<0.4096>)", false)
    }

    @Test
    fun fetchPart_withNonTextSection_shouldIssueRespectiveCommand() {
        val folder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RO)
        folder.open(ImapFolder.OPEN_MODE_RO)
        val message = createImapMessage("1")
        val part = createPart("1.1")
        whenever(imapConnection.readResponse(anyOrNull())).thenReturn(createImapResponse("x OK"))

        folder.fetchPart(message, part, null, mock(), MAX_DOWNLOAD_SIZE)

        verify(imapConnection).sendCommand("UID FETCH 1 (UID BODY.PEEK[1.1])", false)
    }

    @Test
    fun fetchPart_withTextSection_shouldProcessImapResponses() {
        val folder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RO)
        folder.open(ImapFolder.OPEN_MODE_RO)
        val message = createImapMessage("1")
        val part = createPlainTextPart("1.1")
        setupSingleFetchResponseToCallback()

        folder.fetchPart(message, part, null, DefaultBodyFactory(), MAX_DOWNLOAD_SIZE)

        val bodyArgumentCaptor = argumentCaptor<Body>()
        verify(part).body = bodyArgumentCaptor.capture()
        val body = bodyArgumentCaptor.firstValue
        val buffer = Buffer()
        body.writeTo(buffer.outputStream())
        assertEquals("text", buffer.readUtf8())
    }

    @Test
    fun appendMessages_shouldIssueRespectiveCommand() {
        val folder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RW)
        folder.open(ImapFolder.OPEN_MODE_RW)
        val messages = listOf(createImapMessage("1"))
        whenever(imapConnection.readResponse()).thenReturn(createImapResponse("x OK [APPENDUID 1 23]"))

        folder.appendMessages(messages)

        verify(imapConnection).sendCommand("APPEND \"Folder\" () {0}", false)
    }

    @Test
    fun appendMessages_withNegativeResponse_shouldThrow() {
        val folder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RW)
        folder.open(ImapFolder.OPEN_MODE_RW)
        val messages = listOf(createImapMessage("1"))
        whenever(imapConnection.readResponse()).thenReturn(createImapResponse("x NO Can't append to this folder"))

        try {
            folder.appendMessages(messages)
            fail("Expected exception")
        } catch (e: NegativeImapResponseException) {
            assertEquals("APPEND failed", e.message)
            assertEquals("NO", e.lastResponse[0])
        }
    }

    @Test
    fun appendMessages_withBadResponse_shouldThrow() {
        val folder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RW)
        folder.open(ImapFolder.OPEN_MODE_RW)
        val messages = listOf(createImapMessage("1"))
        whenever(imapConnection.readResponse()).thenReturn(createImapResponse("x BAD [TOOBIG] Message too large."))

        try {
            folder.appendMessages(messages)
            fail("Expected exception")
        } catch (e: NegativeImapResponseException) {
            assertEquals("APPEND failed", e.message)
            assertEquals("BAD", e.lastResponse[0])
        }
    }

    @Test
    fun getUidFromMessageId_withMessageIdHeader_shouldIssueUidSearchCommand() {
        val folder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RW)
        folder.open(ImapFolder.OPEN_MODE_RW)
        setupUidSearchResponses("1 OK SEARCH Completed")

        folder.getUidFromMessageId("<00000000.0000000@example.org>")

        assertCommandIssued("UID SEARCH HEADER MESSAGE-ID \"<00000000.0000000@example.org>\"")
    }

    @Test
    fun getUidFromMessageId() {
        val folder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RW)
        folder.open(ImapFolder.OPEN_MODE_RW)
        setupUidSearchResponses("* SEARCH 23")

        val uid = folder.getUidFromMessageId("<00000000.0000000@example.org>")

        assertEquals("23", uid)
    }

    @Test
    fun expunge_shouldIssueExpungeCommand() {
        val folder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RW)

        folder.expunge()

        verify(imapConnection).executeSimpleCommand("EXPUNGE")
    }

    @Test
    fun expungeUids_withUidPlus_shouldIssueUidExpungeCommand() {
        val folder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RW)
        whenever(imapConnection.isUidPlusCapable).thenReturn(true)

        folder.expungeUids(listOf("1"))

        assertCommandWithIdsIssued("UID EXPUNGE 1")
    }

    @Test
    fun expungeUids_withoutUidPlus_shouldIssueExpungeCommand() {
        val folder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RW)
        whenever(imapConnection.isUidPlusCapable).thenReturn(false)

        folder.expungeUids(listOf("1"))

        verify(imapConnection).executeSimpleCommand("EXPUNGE")
    }

    @Test
    fun setFlags_shouldIssueUidStoreCommand() {
        val folder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RW)

        folder.setFlags(setOf(Flag.SEEN), true)

        assertCommandIssued("UID STORE 1:* +FLAGS.SILENT (\\Seen)")
    }

    @Test
    fun search_withFullTextSearchEnabled_shouldIssueRespectiveCommand() {
        val folder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RO)
        setupUidSearchResponses("1 OK SEARCH completed")

        folder.search("query", setOf(Flag.SEEN), emptySet(), true)

        assertCommandIssued("UID SEARCH TEXT \"query\" SEEN")
    }

    @Test
    fun search_withFullTextSearchDisabled_shouldIssueRespectiveCommand() {
        val folder = createFolder("Folder")
        prepareImapFolderForOpen(ImapFolder.OPEN_MODE_RO)
        setupUidSearchResponses("1 OK SEARCH completed")

        folder.search("query", emptySet(), emptySet(), false)

        assertCommandIssued("UID SEARCH OR SUBJECT \"query\" FROM \"query\"")
    }

    @Test
    fun getMessageByUid_returnsNewImapMessageWithUid() {
        val folder = createFolder("Folder")

        val message = folder.getMessage("uid")

        assertEquals("uid", message.uid)
    }

    @Suppress("SameParameterValue")
    private fun createPlainTextPart(serverExtra: String): Part {
        val part = createPart(serverExtra)
        whenever(part.getHeader(MimeHeader.HEADER_CONTENT_TRANSFER_ENCODING)).thenReturn(arrayOf(MimeUtil.ENC_7BIT))
        whenever(part.getHeader(MimeHeader.HEADER_CONTENT_TYPE)).thenReturn(arrayOf("text/plain"))
        return part
    }

    private fun setupSingleFetchResponseToCallback() {
        whenever(imapConnection.readResponse(anyOrNull()))
            .thenAnswer { invocation ->
                val callback = invocation.arguments[0] as ImapResponseCallback
                buildImapFetchResponse(callback)
            }
            .thenAnswer { invocation ->
                val callback = invocation.arguments[0] as ImapResponseCallback
                ImapResponse.newTaggedResponse(callback, "TAG")
            }
    }

    private fun buildImapFetchResponse(callback: ImapResponseCallback): ImapResponse {
        val response = ImapResponse.newContinuationRequest(callback)
        response.add("1")
        response.add("FETCH")
        val fetchList = ImapList()
        fetchList.add("UID")
        fetchList.add("1")
        fetchList.add("BODY")
        fetchList.add("1.1")
        fetchList.add("text")
        response.add(fetchList)
        return response
    }

    private fun extractMessageUids(messages: List<ImapMessage>) = messages.map { it.uid }.toSet()

    private fun createFolder(folderName: String): ImapFolder {
        return ImapFolder(imapStore, folderName, FolderNameCodec.newInstance())
    }

    private fun createImapMessage(uid: String): ImapMessage {
        return mock {
            on { this.uid } doReturn uid
        }
    }

    private fun createImapMessages(vararg uids: String) = uids.map { createImapMessage(it) }

    private fun createPart(serverExtra: String): Part {
        return mock {
            on { this.serverExtra } doReturn serverExtra
        }
    }

    private fun createFetchProfile(vararg items: FetchProfile.Item) = items.toCollection(FetchProfile())

    private fun createMessageRetrievalListener() = mock<MessageRetrievalListener<ImapMessage>>()

    private fun prepareImapFolderForOpen(openMode: Int) {
        whenever(imapStore.connection).thenReturn(imapConnection)
        val imapResponses = listOf(
            createImapResponse("* FLAGS (\\Answered \\Flagged \\Deleted \\Seen \\Draft NonJunk \$MDNSent)"),
            createImapResponse(
                "* OK [PERMANENTFLAGS (\\Answered \\Flagged \\Deleted \\Seen \\Draft NonJunk \$MDNSent \\*)] " +
                    "Flags permitted."
            ),
            createImapResponse("* 23 EXISTS"),
            createImapResponse("* 0 RECENT"),
            createImapResponse("* OK [UIDVALIDITY 1125022061] UIDs valid"),
            createImapResponse("* OK [UIDNEXT 57576] Predicted next UID"),
            if (openMode == ImapFolder.OPEN_MODE_RW) {
                createImapResponse("2 OK [READ-WRITE] Select completed.")
            } else {
                createImapResponse("2 OK [READ-ONLY] Examine completed.")
            }
        )

        if (openMode == ImapFolder.OPEN_MODE_RW) {
            whenever(imapConnection.executeSimpleCommand("SELECT \"Folder\"")).thenReturn(imapResponses)
        } else {
            whenever(imapConnection.executeSimpleCommand("EXAMINE \"Folder\"")).thenReturn(imapResponses)
        }
    }

    @Suppress("SameParameterValue")
    private fun assertCheckOpenErrorMessage(folderName: String, e: MessagingException) {
        assertEquals("Folder $folderName is not open.", e.message)
    }

    private fun assertCommandWithIdsIssued(expectedCommand: String) {
        val commandPrefixCaptor = argumentCaptor<String>()
        val commandSuffixCaptor = argumentCaptor<String>()
        val commandUidsCaptor = argumentCaptor<Set<Long>>()
        verify(imapConnection, atLeastOnce()).executeCommandWithIdSet(
            commandPrefixCaptor.capture(), commandSuffixCaptor.capture(), commandUidsCaptor.capture()
        )

        val commandPrefixes = commandPrefixCaptor.allValues
        val commandSuffixes = commandSuffixCaptor.allValues
        val commandUids = commandUidsCaptor.allValues

        for (i in commandPrefixes.indices) {
            val command = commandPrefixes[i] + " " + ImapUtility.join(",", commandUids[i]) +
                if (commandSuffixes[i].isEmpty()) "" else " " + commandSuffixes[i]

            if (command == expectedCommand) {
                return
            }
        }

        fail("Expected IMAP command not issued: $expectedCommand")
    }

    private fun assertCommandIssued(expectedCommand: String) {
        verify(imapConnection, atLeastOnce()).executeSimpleCommand(expectedCommand)
    }

    private fun setupUidSearchResponses(vararg responses: String) {
        val imapResponses = responses.map { createImapResponse(it) }
        whenever(imapConnection.executeSimpleCommand(startsWith("UID SEARCH"))).thenReturn(imapResponses)
        whenever(imapConnection.executeCommandWithIdSet(startsWith("UID SEARCH"), anyString(), anySet()))
            .thenReturn(imapResponses)
    }

    private fun setupSearchResponses(vararg responses: String) {
        val imapResponses = responses.map { createImapResponse(it) }
        whenever(imapConnection.executeSimpleCommand(startsWith("SEARCH"))).thenReturn(imapResponses)
    }

    @Suppress("SameParameterValue")
    private fun setupCopyResponse(response: String) {
        val imapResponses = listOf(createImapResponse(response))
        whenever(imapConnection.executeCommandWithIdSet(eq(Commands.UID_COPY), anyString(), anySet()))
            .thenReturn(imapResponses)
    }

    companion object {
        private const val MAX_DOWNLOAD_SIZE = -1
    }
}
