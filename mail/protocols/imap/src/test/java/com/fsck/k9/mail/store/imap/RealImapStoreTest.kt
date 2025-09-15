package com.fsck.k9.mail.store.imap

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isSameInstanceAs
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.FolderType
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.oauth.OAuth2TokenProvider
import com.fsck.k9.mail.ssl.TrustedSocketFactory
import com.fsck.k9.mail.store.imap.ImapResponseHelper.createImapResponse
import com.fsck.k9.mail.store.imap.ImapStoreSettings.createExtra
import java.io.IOException
import java.util.ArrayDeque
import java.util.Deque
import net.thunderbird.core.common.exception.MessagingException
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

class RealImapStoreTest {
    private val imapStore = createTestImapStore()

    @Test
    fun `checkSettings() should create ImapConnection and call open()`() {
        val imapConnection = createMockConnection()
        imapStore.enqueueImapConnection(imapConnection)

        imapStore.checkSettings()

        verify(imapConnection).open()
    }

    @Test
    fun `checkSettings() with open throwing an IOException should pass it through`() {
        val ioException = IOException()
        val imapConnection = createMockConnection().stub {
            on { open() } doThrow ioException
        }
        imapStore.enqueueImapConnection(imapConnection)

        assertFailure {
            imapStore.checkSettings()
        }.isSameInstanceAs(ioException)
    }

    @Test
    fun `getFolders() with SPECIAL-USE capability should return special FolderInfo`() {
        val imapConnection = createMockConnection().stub {
            on { hasCapability(Capabilities.LIST_EXTENDED) } doReturn true
            on { hasCapability(Capabilities.SPECIAL_USE) } doReturn true
            on { executeSimpleCommand("""LIST "" "*" RETURN (SPECIAL-USE)""") } doReturn listOf(
                createImapResponse("""* LIST (\HasNoChildren) "/" "INBOX""""),
                createImapResponse("""* LIST (\Noselect \HasChildren) "/" "[Gmail]""""),
                createImapResponse("""* LIST (\HasNoChildren \All) "/" "[Gmail]/All Mail""""),
                createImapResponse("""* LIST (\HasNoChildren \Drafts) "/" "[Gmail]/Drafts""""),
                createImapResponse("""* LIST (\HasNoChildren \Important) "/" "[Gmail]/Important""""),
                createImapResponse("""* LIST (\HasNoChildren \Sent) "/" "[Gmail]/Sent Mail""""),
                createImapResponse("""* LIST (\HasNoChildren \Junk) "/" "[Gmail]/Spam""""),
                createImapResponse("""* LIST (\HasNoChildren \Flagged) "/" "[Gmail]/Starred""""),
                createImapResponse("""* LIST (\HasNoChildren \Trash) "/" "[Gmail]/Trash""""),
                createImapResponse("5 OK Success"),
            )
        }
        imapStore.enqueueImapConnection(imapConnection)

        val folders = imapStore.getFolders()

        val foldersMap = folders.map { it.serverId to it.type }
        assertThat(foldersMap).containsExactly(
            "INBOX" to FolderType.INBOX,
            "[Gmail]/All Mail" to FolderType.ARCHIVE,
            "[Gmail]/Drafts" to FolderType.DRAFTS,
            "[Gmail]/Important" to FolderType.REGULAR,
            "[Gmail]/Sent Mail" to FolderType.SENT,
            "[Gmail]/Spam" to FolderType.SPAM,
            "[Gmail]/Starred" to FolderType.REGULAR,
            "[Gmail]/Trash" to FolderType.TRASH,
        )
    }

    @Test
    fun `getFolders() without SPECIAL-USE capability should use simple LIST command`() {
        val imapConnection = createMockConnection().stub {
            on { hasCapability(Capabilities.LIST_EXTENDED) } doReturn true
            on { hasCapability(Capabilities.SPECIAL_USE) } doReturn false
        }
        imapStore.enqueueImapConnection(imapConnection)

        imapStore.getFolders()

        verify(imapConnection, never()).executeSimpleCommand("""LIST "" "*" RETURN (SPECIAL-USE)""")
        verify(imapConnection).executeSimpleCommand("""LIST "" "*"""")
    }

    @Test
    fun `getFolders() without LIST-EXTENDED capability should use simple LIST command`() {
        val imapConnection = createMockConnection().stub {
            on { hasCapability(Capabilities.LIST_EXTENDED) } doReturn false
            on { hasCapability(Capabilities.SPECIAL_USE) } doReturn true
        }
        imapStore.enqueueImapConnection(imapConnection)

        imapStore.getFolders()

        verify(imapConnection, never()).executeSimpleCommand("""LIST "" "*" RETURN (SPECIAL-USE)""")
        verify(imapConnection).executeSimpleCommand("""LIST "" "*"""")
    }

    @Test
    fun `getFolder() should not corrupt UTF8 folder names`() {
        val imapStore = createTestImapStore(isSubscribedFoldersOnly = false)
        val imapConnection = createMockConnection().stub {
            on { executeSimpleCommand("""LIST "" "*"""") } doReturn listOf(
                createImapResponse("""* LIST () "." "Chèvre"""", true),
                createImapResponse("6 OK Success"),
            )
        }
        imapStore.enqueueImapConnection(imapConnection)

        val folders = imapStore.getFolders()

        assertThat(folders).isNotNull()
        assertThat(folders.map { it.name }).containsExactly("INBOX", "Chèvre")
    }

    @Test
    fun `getFolders() should ignore NoSelect entries`() {
        val imapStore = createTestImapStore(isSubscribedFoldersOnly = false)
        val imapConnection = createMockConnection().stub {
            on { executeSimpleCommand("""LIST "" "*"""") } doReturn listOf(
                createImapResponse("""* LIST () "." "INBOX""""),
                createImapResponse("""* LIST (\Noselect) "." "Folder""""),
                createImapResponse("""* LIST () "." "Folder.SubFolder""""),
                createImapResponse("6 OK Success"),
            )
        }
        imapStore.enqueueImapConnection(imapConnection)

        val folders = imapStore.getFolders()

        assertThat(folders).isNotNull()
        assertThat(folders.map { it.serverId }).containsExactly("INBOX", "Folder.SubFolder")
    }

    @Test
    fun `getFolders() should ignore NonExistent entries`() {
        val imapStore = createTestImapStore(isSubscribedFoldersOnly = false)
        val imapConnection = createMockConnection().stub {
            on { hasCapability(Capabilities.LIST_EXTENDED) } doReturn true
            on { hasCapability(Capabilities.SPECIAL_USE) } doReturn true
            on { executeSimpleCommand("""LIST "" "*" RETURN (SPECIAL-USE)""") } doReturn listOf(
                createImapResponse("""* LIST (\HasNoChildren) "." "INBOX""""),
                createImapResponse("""* LIST (\NonExistent \HasChildren) "." "Folder""""),
                createImapResponse("""* LIST (\HasNoChildren) "." "Folder.SubFolder""""),
                createImapResponse("6 OK Success"),
            )
        }
        imapStore.enqueueImapConnection(imapConnection)

        val folders = imapStore.getFolders()

        assertThat(folders).isNotNull()
        assertThat(folders.map { it.serverId }).containsExactly("INBOX", "Folder.SubFolder")
    }

    @Test
    fun `getFolders() with subscribedFoldersOnly = false`() {
        val imapStore = createTestImapStore(isSubscribedFoldersOnly = false)
        val imapConnection = createMockConnection().stub {
            on { executeSimpleCommand("""LIST "" "*"""") } doReturn listOf(
                createImapResponse("""* LIST (\HasNoChildren) "." "INBOX""""),
                createImapResponse("""* LIST (\HasNoChildren) "." "Folder""""),
                createImapResponse("6 OK Success"),
            )
        }
        imapStore.enqueueImapConnection(imapConnection)

        val folders = imapStore.getFolders()

        assertThat(folders).isNotNull()
        assertThat(folders.map { it.serverId }).containsExactly("INBOX", "Folder")
    }

    @Test
    fun `getFolders() with subscribedFoldersOnly = true should only return existing subscribed folders`() {
        val imapStore = createTestImapStore(isSubscribedFoldersOnly = true)
        val imapConnection = createMockConnection().stub {
            on { executeSimpleCommand("""LSUB "" "*"""") } doReturn listOf(
                createImapResponse("""* LSUB (\HasNoChildren) "." "INBOX""""),
                createImapResponse("""* LSUB (\Noselect \HasChildren) "." "Folder""""),
                createImapResponse("""* LSUB (\HasNoChildren) "." "Folder.SubFolder""""),
                createImapResponse("""* LSUB (\HasNoChildren) "." "SubscribedFolderThatHasBeenDeleted""""),
                createImapResponse("5 OK Success"),
            )
            on { executeSimpleCommand("""LIST "" "*"""") } doReturn listOf(
                createImapResponse("""* LIST (\HasNoChildren) "." "INBOX""""),
                createImapResponse("""* LIST (\Noselect \HasChildren) "." "Folder""""),
                createImapResponse("""* LIST (\HasNoChildren) "." "Folder.SubFolder""""),
                createImapResponse("6 OK Success"),
            )
        }
        imapStore.enqueueImapConnection(imapConnection)

        val folders = imapStore.getFolders()

        assertThat(folders).isNotNull()
        assertThat(folders.map { it.serverId }).containsExactly("INBOX", "Folder.SubFolder")
    }

    @Test
    fun `getFolders() with namespace prefix`() {
        val imapConnection = createMockConnection().stub {
            on { executeSimpleCommand("""LIST "" "INBOX.*"""") } doReturn listOf(
                createImapResponse("""* LIST () "." "INBOX""""),
                createImapResponse("""* LIST () "." "INBOX.FolderOne""""),
                createImapResponse("""* LIST () "." "INBOX.FolderTwo""""),
                createImapResponse("5 OK Success"),
            )
        }
        imapStore.enqueueImapConnection(imapConnection)
        imapStore.setTestCombinedPrefix("INBOX.")

        val folders = imapStore.getFolders()

        assertThat(folders).isNotNull()
        assertThat(folders.map { it.serverId }).containsExactly("INBOX", "INBOX.FolderOne", "INBOX.FolderTwo")
        assertThat(folders.map { it.name }).containsExactly("INBOX", "FolderOne", "FolderTwo")
    }

    @Test
    fun `getFolders() with folder not matching namespace prefix`() {
        val imapConnection = createMockConnection().stub {
            on { executeSimpleCommand("""LIST "" "INBOX.*"""") } doReturn listOf(
                createImapResponse("""* LIST () "." "INBOX""""),
                createImapResponse("""* LIST () "." "INBOX.FolderOne""""),
                createImapResponse("""* LIST () "." "FolderTwo""""),
                createImapResponse("5 OK Success"),
            )
        }
        imapStore.enqueueImapConnection(imapConnection)
        imapStore.setTestCombinedPrefix("INBOX.")

        val folders = imapStore.getFolders()

        assertThat(folders).isNotNull()
        assertThat(folders.map { it.serverId }).containsExactly("INBOX", "INBOX.FolderOne", "FolderTwo")
        assertThat(folders.map { it.name }).containsExactly("INBOX", "FolderOne", "FolderTwo")
    }

    @Test
    fun `getFolders() with duplicate folder names should remove duplicates and keep FolderType`() {
        val imapConnection = createMockConnection().stub {
            on { hasCapability(Capabilities.LIST_EXTENDED) } doReturn true
            on { hasCapability(Capabilities.SPECIAL_USE) } doReturn true
            on { executeSimpleCommand("""LIST "" "*" RETURN (SPECIAL-USE)""") } doReturn listOf(
                createImapResponse("""* LIST () "." "INBOX""""),
                createImapResponse("""* LIST (\HasNoChildren) "." "Junk""""),
                createImapResponse("""* LIST (\Junk) "." "Junk""""),
                createImapResponse("""* LIST (\HasNoChildren) "." "Junk""""),
                createImapResponse("5 OK Success"),
            )
        }
        imapStore.enqueueImapConnection(imapConnection)

        val folders = imapStore.getFolders()

        assertThat(folders.map { it.serverId to it.type }).containsExactly(
            "INBOX" to FolderType.INBOX,
            "Junk" to FolderType.SPAM,
        )
    }

    @Test
    fun `getFolders() without exception should leave ImapConnection open`() {
        val imapConnection = createMockConnection().stub {
            on { executeSimpleCommand(anyString()) } doReturn listOf(createImapResponse("5 OK Success"))
        }
        imapStore.enqueueImapConnection(imapConnection)

        imapStore.getFolders()

        verify(imapConnection, never()).close()
    }

    @Test
    fun `getFolders() with IOException should close ImapConnection`() {
        val imapConnection = createMockConnection().stub {
            on { executeSimpleCommand("""LIST "" "*"""") } doThrow IOException::class
        }
        imapStore.enqueueImapConnection(imapConnection)

        assertFailure {
            imapStore.getFolders()
        }.isInstanceOf<MessagingException>()

        verify(imapConnection).close()
    }

    @Test
    fun `getConnection() should create ImapConnection`() {
        val imapConnection = createMockConnection()
        imapStore.enqueueImapConnection(imapConnection)

        val result = imapStore.getConnection()

        assertThat(result).isSameInstanceAs(imapConnection)
    }

    @Test
    fun `getConnection() called twice without release should create two ImapConnection instances`() {
        val imapConnectionOne = createMockConnection()
        val imapConnectionTwo = createMockConnection()
        imapStore.enqueueImapConnection(imapConnectionOne)
        imapStore.enqueueImapConnection(imapConnectionTwo)

        val resultOne = imapStore.getConnection()
        val resultTwo = imapStore.getConnection()

        assertThat(resultOne).isSameInstanceAs(imapConnectionOne)
        assertThat(resultTwo).isSameInstanceAs(imapConnectionTwo)
    }

    @Test
    fun `getConnection() called after release should return cached ImapConnection`() {
        val imapConnection = createMockConnection().stub {
            on { isConnected } doReturn true
        }
        imapStore.enqueueImapConnection(imapConnection)

        val connection = imapStore.getConnection()
        imapStore.releaseConnection(connection)

        val result = imapStore.getConnection()

        assertThat(result).isSameInstanceAs(imapConnection)
    }

    @Test
    fun `getConnection() called after release with closed connection should return new ImapConnection instance`() {
        val imapConnectionOne = createMockConnection()
        val imapConnectionTwo = createMockConnection()
        imapStore.enqueueImapConnection(imapConnectionOne)
        imapStore.enqueueImapConnection(imapConnectionTwo)

        imapStore.getConnection()
        imapConnectionOne.stub {
            on { isConnected } doReturn false
        }
        imapStore.releaseConnection(imapConnectionOne)

        val result = imapStore.getConnection()

        assertThat(result).isSameInstanceAs(imapConnectionTwo)
    }

    @Test
    fun `getConnection() with dead connection in pool should return new ImapConnection instance`() {
        val imapConnectionOne = createMockConnection()
        val imapConnectionTwo = createMockConnection()
        imapStore.enqueueImapConnection(imapConnectionOne)
        imapStore.enqueueImapConnection(imapConnectionTwo)

        imapStore.getConnection()
        imapConnectionOne.stub {
            on { isConnected } doReturn true
            on { executeSimpleCommand(Commands.NOOP) } doThrow IOException::class
        }
        imapStore.releaseConnection(imapConnectionOne)

        val result = imapStore.getConnection()

        assertThat(result).isSameInstanceAs(imapConnectionTwo)
    }

    @Test
    fun `getConnection() with connection in pool and closeAllConnections() should return new ImapConnection instance`() {
        val imapConnectionOne = createMockConnection(1)
        val imapConnectionTwo = createMockConnection(2)
        imapStore.enqueueImapConnection(imapConnectionOne)
        imapStore.enqueueImapConnection(imapConnectionTwo)

        imapStore.getConnection()
        imapConnectionOne.stub {
            on { isConnected } doReturn true
        }
        imapStore.releaseConnection(imapConnectionOne)
        imapStore.closeAllConnections()

        val result = imapStore.getConnection()

        assertThat(result).isSameInstanceAs(imapConnectionTwo)
    }

    @Test
    fun `getConnection() with connection outside of pool and closeAllConnections() should return new ImapConnection instance`() {
        val imapConnectionOne = createMockConnection(1)
        val imapConnectionTwo = createMockConnection(2)
        imapStore.enqueueImapConnection(imapConnectionOne)
        imapStore.enqueueImapConnection(imapConnectionTwo)

        imapStore.getConnection()
        imapConnectionOne.stub {
            on { isConnected } doReturn true
        }
        imapStore.closeAllConnections()
        imapStore.releaseConnection(imapConnectionOne)

        val result = imapStore.getConnection()

        assertThat(result).isSameInstanceAs(imapConnectionTwo)
    }

    private fun createMockConnection(connectionGeneration: Int = 1): ImapConnection {
        return mock {
            on { this.connectionGeneration } doReturn connectionGeneration
        }
    }

    private fun createServerSettings(): ServerSettings {
        return ServerSettings(
            type = "imap",
            host = "imap.example.org",
            port = 143,
            connectionSecurity = ConnectionSecurity.NONE,
            authenticationType = AuthType.PLAIN,
            username = "user",
            password = "password",
            clientCertificateAlias = null,
            extra = createExtra(
                autoDetectNamespace = true,
                pathPrefix = null,
                useCompression = false,
                sendClientInfo = false,
            ),
        )
    }

    private fun createTestImapStore(
        isSubscribedFoldersOnly: Boolean = false,
    ): TestImapStore {
        return TestImapStore(
            serverSettings = createServerSettings(),
            config = createImapStoreConfig(isSubscribedFoldersOnly),
            trustedSocketFactory = mock(),
            oauth2TokenProvider = null,
        )
    }

    private fun createImapStoreConfig(
        isSubscribedFoldersOnly: Boolean,
    ): ImapStoreConfig {
        return object : ImapStoreConfig {
            override val logLabel: String = "irrelevant"
            override fun isSubscribedFoldersOnly(): Boolean = isSubscribedFoldersOnly
            override fun isExpungeImmediately(): Boolean = true
            override fun clientInfo() = ImapClientInfo(appName = "irrelevant", appVersion = "irrelevant")
        }
    }

    private class TestImapStore(
        serverSettings: ServerSettings,
        config: ImapStoreConfig,
        trustedSocketFactory: TrustedSocketFactory,
        oauth2TokenProvider: OAuth2TokenProvider?,
    ) : RealImapStore(
        serverSettings,
        config,
        trustedSocketFactory,
        oauth2TokenProvider,
    ) {
        private val imapConnections: Deque<ImapConnection> = ArrayDeque()
        private var testCombinedPrefix: String? = null

        override fun createImapConnection(): ImapConnection {
            if (imapConnections.isEmpty()) {
                throw AssertionError("Unexpectedly tried to create an ImapConnection instance")
            }

            return imapConnections.pop()
        }

        fun enqueueImapConnection(imapConnection: ImapConnection) {
            imapConnections.add(imapConnection)
        }

        override val combinedPrefix: String?
            get() = testCombinedPrefix ?: super.combinedPrefix

        fun setTestCombinedPrefix(prefix: String?) {
            testCombinedPrefix = prefix
        }
    }
}
