package com.fsck.k9.mail.store.imap

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.prop
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.AuthenticationFailedException
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.FolderType
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.folders.FolderFetcherException
import com.fsck.k9.mail.folders.FolderServerId
import com.fsck.k9.mail.folders.RemoteFolder
import com.fsck.k9.mail.store.imap.ImapResponseHelper.createImapResponseList
import com.fsck.k9.mail.testing.security.FakeTrustManager
import com.fsck.k9.mail.testing.security.SimpleTrustedSocketFactory
import kotlin.test.Test

class ImapFolderFetcherTest {
    private val fakeTrustManager = FakeTrustManager()
    private val trustedSocketFactory = SimpleTrustedSocketFactory(fakeTrustManager)
    private val fakeImapStore = FakeImapStore()
    private val folderFetcher = ImapFolderFetcher(
        trustedSocketFactory = trustedSocketFactory,
        oAuth2TokenProviderFactory = null,
        clientInfoAppName = "irrelevant",
        clientInfoAppVersion = "irrelevant",
        imapStoreFactory = { _, _, _, _ ->
            fakeImapStore
        },
    )
    private val serverSettings = ServerSettings(
        type = "imap",
        host = "irrelevant",
        port = 9999,
        connectionSecurity = ConnectionSecurity.NONE,
        authenticationType = AuthType.PLAIN,
        username = "irrelevant",
        password = "irrelevant",
        clientCertificateAlias = null,
        extra = ImapStoreSettings.createExtra(
            autoDetectNamespace = true,
            pathPrefix = null,
            useCompression = false,
            sendClientInfo = false,
        ),
    )

    @Suppress("LongMethod")
    @Test
    fun `regular folder list`() {
        fakeImapStore.getFoldersAction = {
            listOf(
                FolderListItem(
                    serverId = "INBOX",
                    name = "INBOX",
                    type = FolderType.INBOX,
                    oldServerId = "INBOX",
                ),
                FolderListItem(
                    serverId = "[Gmail]/All Mail",
                    name = "[Gmail]/All Mail",
                    type = FolderType.ARCHIVE,
                    oldServerId = "[Gmail]/All Mail",
                ),
                FolderListItem(
                    serverId = "[Gmail]/Drafts",
                    name = "[Gmail]/Drafts",
                    type = FolderType.DRAFTS,
                    oldServerId = "[Gmail]/Drafts",
                ),
                FolderListItem(
                    serverId = "[Gmail]/Important",
                    name = "[Gmail]/Important",
                    type = FolderType.REGULAR,
                    oldServerId = "[Gmail]/Important",
                ),
                FolderListItem(
                    serverId = "[Gmail]/Sent Mail",
                    name = "[Gmail]/Sent Mail",
                    type = FolderType.SENT,
                    oldServerId = "[Gmail]/Sent Mail",
                ),
                FolderListItem(
                    serverId = "[Gmail]/Spam",
                    name = "[Gmail]/Spam",
                    type = FolderType.SPAM,
                    oldServerId = "[Gmail]/Spam",
                ),
                FolderListItem(
                    serverId = "[Gmail]/Starred",
                    name = "[Gmail]/Starred",
                    type = FolderType.REGULAR,
                    oldServerId = "[Gmail]/Starred",
                ),
                FolderListItem(
                    serverId = "[Gmail]/Trash",
                    name = "[Gmail]/Trash",
                    type = FolderType.TRASH,
                    oldServerId = "[Gmail]/Trash",
                ),
            )
        }

        val folders = folderFetcher.getFolders(serverSettings, authStateStorage = null)

        assertThat(folders).containsExactly(
            RemoteFolder(
                serverId = FolderServerId("INBOX"),
                displayName = "INBOX",
                type = FolderType.INBOX,
            ),
            RemoteFolder(
                serverId = FolderServerId("[Gmail]/All Mail"),
                displayName = "[Gmail]/All Mail",
                type = FolderType.ARCHIVE,
            ),
            RemoteFolder(
                serverId = FolderServerId("[Gmail]/Drafts"),
                displayName = "[Gmail]/Drafts",
                type = FolderType.DRAFTS,
            ),
            RemoteFolder(
                serverId = FolderServerId("[Gmail]/Important"),
                displayName = "[Gmail]/Important",
                type = FolderType.REGULAR,
            ),
            RemoteFolder(
                serverId = FolderServerId("[Gmail]/Sent Mail"),
                displayName = "[Gmail]/Sent Mail",
                type = FolderType.SENT,
            ),
            RemoteFolder(
                serverId = FolderServerId("[Gmail]/Spam"),
                displayName = "[Gmail]/Spam",
                type = FolderType.SPAM,
            ),
            RemoteFolder(
                serverId = FolderServerId("[Gmail]/Starred"),
                displayName = "[Gmail]/Starred",
                type = FolderType.REGULAR,
            ),
            RemoteFolder(
                serverId = FolderServerId("[Gmail]/Trash"),
                displayName = "[Gmail]/Trash",
                type = FolderType.TRASH,
            ),
        )
        assertThat(fakeImapStore.hasOpenConnections).isFalse()
    }

    @Test
    fun `folder without oldServerId should be ignored`() {
        fakeImapStore.getFoldersAction = {
            listOf(
                FolderListItem(
                    serverId = "ünicode",
                    name = "ünicode",
                    type = FolderType.REGULAR,
                    oldServerId = null,
                ),
                FolderListItem(
                    serverId = "INBOX",
                    name = "INBOX",
                    type = FolderType.INBOX,
                    oldServerId = "INBOX",
                ),
            )
        }

        val folders = folderFetcher.getFolders(serverSettings, authStateStorage = null)

        assertThat(folders).containsExactly(
            RemoteFolder(
                serverId = FolderServerId("INBOX"),
                displayName = "INBOX",
                type = FolderType.INBOX,
            ),
        )

        assertThat(fakeImapStore.hasOpenConnections).isFalse()
    }

    @Test
    fun `authentication error should throw FolderFetcherException with server message`() {
        fakeImapStore.getFoldersAction = {
            throw AuthenticationFailedException(message = "Authentication failed", messageFromServer = "Server error")
        }

        assertFailure {
            folderFetcher.getFolders(serverSettings, authStateStorage = null)
        }.isInstanceOf<FolderFetcherException>()
            .prop(FolderFetcherException::messageFromServer).isEqualTo("Server error")

        assertThat(fakeImapStore.hasOpenConnections).isFalse()
    }

    @Test
    fun `NegativeImapResponseException should throw FolderFetcherException with reply text as messageFromServer`() {
        fakeImapStore.getFoldersAction = {
            throw NegativeImapResponseException(
                message = "irrelevant",
                responses = createImapResponseList("x NO [NOPERM] Access denied"),
            )
        }

        assertFailure {
            folderFetcher.getFolders(serverSettings, authStateStorage = null)
        }.isInstanceOf<FolderFetcherException>()
            .prop(FolderFetcherException::messageFromServer).isEqualTo("Access denied")

        assertThat(fakeImapStore.hasOpenConnections).isFalse()
    }

    @Test
    fun `unexpected exception should throw FolderFetcherException`() {
        fakeImapStore.getFoldersAction = {
            error("unexpected")
        }

        assertFailure {
            folderFetcher.getFolders(serverSettings, authStateStorage = null)
        }.isInstanceOf<FolderFetcherException>()
            .prop(FolderFetcherException::messageFromServer).isNull()

        assertThat(fakeImapStore.hasOpenConnections).isFalse()
    }
}
