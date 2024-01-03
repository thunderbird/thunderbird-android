package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.feature.account.common.data.InMemoryAccountStateRepository
import app.k9mail.feature.account.common.domain.AccountDomainContract.AccountStateRepository
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.SpecialFolderOption
import app.k9mail.feature.account.setup.domain.DomainContract.UseCase
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.FolderType
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.folders.FolderFetcher
import com.fsck.k9.mail.folders.FolderServerId
import com.fsck.k9.mail.folders.RemoteFolder
import com.fsck.k9.mail.oauth.AuthStateStorage
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GetSpecialFolderOptionsTest {

    @Test
    fun `should fail when no incoming server settings found`() = runTest {
        val testSubject = createTestSubject(
            accountStateRepository = InMemoryAccountStateRepository(
                state = AccountState(
                    incomingServerSettings = null,
                ),
            ),
        )

        assertFailure { testSubject() }
            .isInstanceOf<IllegalStateException>()
            .hasMessage("No incoming server settings available")
    }

    @Test
    fun `should map remote folders to Folders`() = runTest {
        val testSubject = createTestSubject(
            folderFetcher = FakeFolderFetcher(folders = FOLDERS),
            accountStateRepository = InMemoryAccountStateRepository(
                state = AccountState(
                    incomingServerSettings = SERVER_SETTINGS,
                ),
            ),
        )

        val folders = testSubject()

        assertThat(folders.archiveSpecialFolderOptions).containsExactly(
            *getArrayOfFolders(
                SpecialFolderOption.Special(
                    remoteFolder = ARCHIVE_FOLDER_1,
                    isAutomatic = true,
                ),
            ),
        )
        assertThat(folders.draftsSpecialFolderOptions).containsExactly(
            *getArrayOfFolders(
                SpecialFolderOption.Special(
                    remoteFolder = DRAFTS_FOLDER_1,
                    isAutomatic = true,
                ),
            ),
        )
        assertThat(folders.sentSpecialFolderOptions).containsExactly(
            *getArrayOfFolders(
                SpecialFolderOption.Special(
                    remoteFolder = SENT_FOLDER_1,
                    isAutomatic = true,
                ),
            ),
        )
        assertThat(folders.spamSpecialFolderOptions).containsExactly(
            *getArrayOfFolders(
                SpecialFolderOption.Special(
                    remoteFolder = SPAM_FOLDER_1,
                    isAutomatic = true,
                ),
            ),
        )
        assertThat(folders.trashSpecialFolderOptions).containsExactly(
            *getArrayOfFolders(
                SpecialFolderOption.Special(
                    remoteFolder = TRASH_FOLDER_1,
                    isAutomatic = true,
                ),
            ),
        )
    }

    @Test
    fun `should map remote folders to Folders and take first special folder for type`() = runTest {
        val testSubject = createTestSubject(
            folderFetcher = FakeFolderFetcher(
                folders = listOf(
                    ARCHIVE_FOLDER_2,
                    ARCHIVE_FOLDER_1,
                    DRAFTS_FOLDER_2,
                    DRAFTS_FOLDER_1,
                    SENT_FOLDER_2,
                    SENT_FOLDER_1,
                    SPAM_FOLDER_2,
                    SPAM_FOLDER_1,
                    TRASH_FOLDER_2,
                    TRASH_FOLDER_1,
                    REGULAR_FOLDER_1,
                    REGULAR_FOLDER_2,
                ),
            ),
            accountStateRepository = InMemoryAccountStateRepository(
                state = AccountState(
                    incomingServerSettings = SERVER_SETTINGS,
                ),
            ),
        )

        val folders = testSubject()

        assertThat(folders.archiveSpecialFolderOptions[0]).isEqualTo(
            SpecialFolderOption.Special(
                remoteFolder = ARCHIVE_FOLDER_1,
                isAutomatic = true,
            ),
        )
        assertThat(folders.draftsSpecialFolderOptions[0]).isEqualTo(
            SpecialFolderOption.Special(
                remoteFolder = DRAFTS_FOLDER_1,
                isAutomatic = true,
            ),
        )
        assertThat(folders.sentSpecialFolderOptions[0]).isEqualTo(
            SpecialFolderOption.Special(
                remoteFolder = SENT_FOLDER_1,
                isAutomatic = true,
            ),
        )
        assertThat(folders.spamSpecialFolderOptions[0]).isEqualTo(
            SpecialFolderOption.Special(
                remoteFolder = SPAM_FOLDER_1,
                isAutomatic = true,
            ),
        )
        assertThat(folders.trashSpecialFolderOptions[0]).isEqualTo(
            SpecialFolderOption.Special(
                remoteFolder = TRASH_FOLDER_1,
                isAutomatic = true,
            ),
        )
    }

    @Test
    fun `should map remote folders to Folders when no special folder present`() = runTest {
        val testSubject = createTestSubject(
            folderFetcher = FakeFolderFetcher(
                folders = listOf(
                    REGULAR_FOLDER_1,
                    REGULAR_FOLDER_2,
                ),
            ),
            accountStateRepository = InMemoryAccountStateRepository(
                state = AccountState(
                    incomingServerSettings = SERVER_SETTINGS,
                ),
            ),
        )
        val expectedSpecialFolderOptions = listOf(
            SpecialFolderOption.None(isAutomatic = true),
            SpecialFolderOption.None(),
            SpecialFolderOption.Regular(REGULAR_FOLDER_1),
            SpecialFolderOption.Regular(REGULAR_FOLDER_2),
        ).toTypedArray()

        val folders = testSubject()

        assertThat(folders.archiveSpecialFolderOptions).containsExactly(
            *expectedSpecialFolderOptions,
        )
        assertThat(folders.draftsSpecialFolderOptions).containsExactly(
            *expectedSpecialFolderOptions,
        )
        assertThat(folders.sentSpecialFolderOptions).containsExactly(
            *expectedSpecialFolderOptions,
        )
        assertThat(folders.spamSpecialFolderOptions).containsExactly(
            *expectedSpecialFolderOptions,
        )
        assertThat(folders.trashSpecialFolderOptions).containsExactly(
            *expectedSpecialFolderOptions,
        )
    }

    private companion object {
        fun createTestSubject(
            folderFetcher: FolderFetcher = FakeFolderFetcher(),
            accountStateRepository: AccountStateRepository = InMemoryAccountStateRepository(),
        ): UseCase.GetSpecialFolderOptions {
            return GetSpecialFolderOptions(
                folderFetcher = folderFetcher,
                accountStateRepository = accountStateRepository,
                authStateStorage = accountStateRepository as AuthStateStorage,
            )
        }

        val ARCHIVE_FOLDER_1 = RemoteFolder(
            serverId = FolderServerId("Archive"),
            displayName = "Archive",
            type = FolderType.ARCHIVE,
        )

        val ARCHIVE_FOLDER_2 = RemoteFolder(
            serverId = FolderServerId("Archive2"),
            displayName = "Archive2",
            type = FolderType.ARCHIVE,
        )

        val DRAFTS_FOLDER_1 = RemoteFolder(
            serverId = FolderServerId("Drafts"),
            displayName = "Drafts",
            type = FolderType.DRAFTS,
        )

        val DRAFTS_FOLDER_2 = RemoteFolder(
            serverId = FolderServerId("Drafts2"),
            displayName = "Drafts2",
            type = FolderType.DRAFTS,
        )

        val SENT_FOLDER_1 = RemoteFolder(
            serverId = FolderServerId("Sent"),
            displayName = "Sent",
            type = FolderType.SENT,
        )

        val SENT_FOLDER_2 = RemoteFolder(
            serverId = FolderServerId("Sent2"),
            displayName = "Sent2",
            type = FolderType.SENT,
        )

        val SPAM_FOLDER_1 = RemoteFolder(
            serverId = FolderServerId("Spam"),
            displayName = "Spam",
            type = FolderType.SPAM,
        )

        val SPAM_FOLDER_2 = RemoteFolder(
            serverId = FolderServerId("Spam2"),
            displayName = "Spam2",
            type = FolderType.SPAM,
        )

        val TRASH_FOLDER_1 = RemoteFolder(
            serverId = FolderServerId("Trash"),
            displayName = "Trash",
            type = FolderType.TRASH,
        )

        val TRASH_FOLDER_2 = RemoteFolder(
            serverId = FolderServerId("Trash2"),
            displayName = "Trash2",
            type = FolderType.TRASH,
        )

        val REGULAR_FOLDER_1 = RemoteFolder(
            serverId = FolderServerId("Regular1"),
            displayName = "Regular1",
            type = FolderType.REGULAR,
        )

        val REGULAR_FOLDER_2 = RemoteFolder(
            serverId = FolderServerId("Regular2"),
            displayName = "Regular2",
            type = FolderType.REGULAR,
        )

        val FOLDERS = listOf(
            ARCHIVE_FOLDER_1,
            DRAFTS_FOLDER_1,
            SENT_FOLDER_1,
            SPAM_FOLDER_1,
            TRASH_FOLDER_1,
            REGULAR_FOLDER_1,
            REGULAR_FOLDER_2,
        )

        fun getArrayOfFolders(defaultSpecialFolderOption: SpecialFolderOption?): Array<SpecialFolderOption> {
            return listOfNotNull(
                defaultSpecialFolderOption,
                SpecialFolderOption.None(),
                SpecialFolderOption.Special(
                    remoteFolder = ARCHIVE_FOLDER_1,
                ),
                SpecialFolderOption.Special(
                    remoteFolder = DRAFTS_FOLDER_1,
                ),
                SpecialFolderOption.Regular(REGULAR_FOLDER_1),
                SpecialFolderOption.Regular(REGULAR_FOLDER_2),
                SpecialFolderOption.Special(
                    remoteFolder = SENT_FOLDER_1,
                ),
                SpecialFolderOption.Special(
                    remoteFolder = SPAM_FOLDER_1,
                ),
                SpecialFolderOption.Special(
                    remoteFolder = TRASH_FOLDER_1,
                ),
            ).toTypedArray()
        }

        val SERVER_SETTINGS = ServerSettings(
            type = "imap",
            host = "imap.example.org",
            port = 993,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            username = "example",
            password = "password",
            clientCertificateAlias = null,
            authenticationType = AuthType.PLAIN,
        )
    }
}
