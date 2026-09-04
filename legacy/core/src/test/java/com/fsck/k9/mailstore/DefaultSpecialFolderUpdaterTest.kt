package com.fsck.k9.mailstore

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto
import net.thunderbird.feature.account.storage.profile.ProfileDto
import net.thunderbird.feature.mail.folder.api.FolderDetails
import net.thunderbird.feature.mail.folder.api.FolderType
import net.thunderbird.feature.mail.folder.api.RemoteFolder
import net.thunderbird.feature.mail.folder.api.SpecialFolderSelection
import net.thunderbird.feature.mail.folder.api.data.FolderError
import net.thunderbird.feature.mail.folder.api.data.repository.FolderDetailsRepository
import net.thunderbird.feature.mail.folder.api.data.repository.PartialUpdatableFolderDetails
import net.thunderbird.feature.mail.folder.api.data.repository.RemoteFolderQueryRepository
import org.junit.Test

@OptIn(ExperimentalUuidApi::class, ExperimentalCoroutinesApi::class)
class DefaultSpecialFolderUpdaterTest {
    private val accountId = AccountIdFactory.create()
    private val account = createAccount(accountId)
    private val accountManager = FakeLegacyAccountManager(initialAccounts = listOf(account))
    private val remoteFolderQueryRepository = FakeRemoteFolderQueryRepository()
    private val folderDetailsRepository = FakeFolderDetailsRepository()
    private val specialFolderSelectionStrategy = SpecialFolderSelectionStrategy()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val testDispatcher = UnconfinedTestDispatcher()
    private val coroutineScope = CoroutineScope(testDispatcher)

    private val subject = DefaultSpecialFolderUpdater(
        accountManager = accountManager,
        remoteFolderQueryRepository = remoteFolderQueryRepository,
        folderDetailsRepository = folderDetailsRepository,
        specialFolderSelectionStrategy = specialFolderSelectionStrategy,
        accountId = accountId,
        coroutineScope = coroutineScope,
        logger = TestLogger(),
        ioDispatcher = testDispatcher,
    )

    @Test
    fun `updateSpecialFoldersSync should update all folders synchronously`() = runTest {
        // Arrange
        val inboxFolder = createRemoteFolder(id = 1, type = FolderType.INBOX, serverId = "INBOX")
        val draftsFolder = createRemoteFolder(id = 2, type = FolderType.DRAFTS, serverId = "Drafts")
        val sentFolder = createRemoteFolder(id = 3, type = FolderType.SENT, serverId = "Sent")
        val trashFolder = createRemoteFolder(id = 4, type = FolderType.TRASH, serverId = "Trash")
        val archiveFolder = createRemoteFolder(id = 5, type = FolderType.ARCHIVE, serverId = "Archive")
        val spamFolder = createRemoteFolder(id = 6, type = FolderType.SPAM, serverId = "Spam")

        remoteFolderQueryRepository.remoteFolders =
            listOf(inboxFolder, draftsFolder, sentFolder, trashFolder, archiveFolder, spamFolder)

        // Act
        subject.updateSpecialFoldersSync()

        // Assert
        val updatedAccount = accountManager.getByIdSync(accountId)!!
        assertThat(updatedAccount.inboxFolderId).isEqualTo(1L)
        assertThat(updatedAccount.draftsFolderId).isEqualTo(2L)
        assertThat(updatedAccount.sentFolderId).isEqualTo(3L)
        assertThat(updatedAccount.trashFolderId).isEqualTo(4L)
        assertThat(updatedAccount.archiveFolderId).isEqualTo(5L)
        assertThat(updatedAccount.spamFolderId).isEqualTo(6L)
    }

    @Test
    fun `updateSpecialFolders should update all folders asynchronously`() = runTest {
        // Arrange
        val inboxFolder = createRemoteFolder(id = 1, type = FolderType.INBOX, serverId = "INBOX")
        val draftsFolder = createRemoteFolder(id = 2, type = FolderType.DRAFTS, serverId = "Drafts")
        remoteFolderQueryRepository.remoteFolders = listOf(inboxFolder, draftsFolder)

        // Act
        subject.updateSpecialFolders()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val updatedAccount = accountManager.getByIdSync(accountId)!!
        assertThat(updatedAccount.inboxFolderId).isEqualTo(1L)
        assertThat(updatedAccount.draftsFolderId).isEqualTo(2L)
    }

    @Test
    fun `updateSpecialFoldersSync should update imported folders by server ID`() = runTest {
        // Arrange
        val accountWithImports = account.copy(
            importedDraftsFolder = "ImportedDrafts",
            draftsFolderSelection = SpecialFolderSelection.MANUAL,
        )
        accountManager.updateSync(accountWithImports)

        val draftsFolder = createRemoteFolder(id = 10, type = FolderType.REGULAR, serverId = "ImportedDrafts")
        remoteFolderQueryRepository.remoteFolders = listOf(draftsFolder)

        // Act
        subject.updateSpecialFoldersSync()

        // Assert
        val updatedAccount = accountManager.getByIdSync(accountId)!!
        assertThat(updatedAccount.draftsFolderId).isEqualTo(10L)
        assertThat(updatedAccount.importedDraftsFolder).isNull()
    }

    @Test
    fun `updateSpecialFoldersSync should fallback to automatic selection if manual folder missing`() = runTest {
        // Arrange
        val accountWithManual = account.copy(
            draftsFolderId = 99L,
            draftsFolderSelection = SpecialFolderSelection.MANUAL,
        )
        accountManager.updateSync(accountWithManual)

        // Remote folders do NOT include ID 99, but has a folder named "Drafts"
        val draftsFolder = createRemoteFolder(id = 2, type = FolderType.DRAFTS, serverId = "Drafts")
        remoteFolderQueryRepository.remoteFolders = listOf(draftsFolder)

        // Act
        subject.updateSpecialFoldersSync()

        // Assert
        val updatedAccount = accountManager.getByIdSync(accountId)!!
        assertThat(updatedAccount.draftsFolderId).isEqualTo(2L)
        assertThat(updatedAccount.draftsFolderSelection).isEqualTo(SpecialFolderSelection.AUTOMATIC)
    }

    @Test
    fun `updateSpecialFoldersSync for POP3 should only update Inbox`() = runTest {
        // Arrange
        val pop3Account = account.copy(
            incomingServerSettings = account.incomingServerSettings.copy(type = Protocols.POP3),
        )
        accountManager.updateSync(pop3Account)

        val inboxFolder = createRemoteFolder(id = 1, type = FolderType.INBOX, serverId = "INBOX")
        val draftsFolder = createRemoteFolder(id = 2, type = FolderType.DRAFTS, serverId = "Drafts")
        remoteFolderQueryRepository.remoteFolders = listOf(inboxFolder, draftsFolder)

        // Act
        subject.updateSpecialFoldersSync()

        // Assert
        val updatedAccount = accountManager.getByIdSync(accountId)!!
        assertThat(updatedAccount.inboxFolderId).isEqualTo(1L)
        assertThat(updatedAccount.draftsFolderId).isNull()
    }

    private fun createAccount(id: AccountId): LegacyAccount {
        val profile = ProfileDto(
            id = id,
            name = "name",
            color = 0,
            avatar = AvatarDto(AvatarTypeDto.MONOGRAM, "A", null, null),
        )
        val incoming = ServerSettings(
            type = Protocols.IMAP,
            host = "example.com",
            port = 993,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "user",
            password = "pass",
            clientCertificateAlias = null,
        )
        val outgoing = ServerSettings(
            type = "smtp",
            host = "example.com",
            port = 587,
            connectionSecurity = ConnectionSecurity.STARTTLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "user",
            password = "pass",
            clientCertificateAlias = null,
        )
        return LegacyAccount(
            id = id,
            name = "acc",
            email = "user@example.com",
            profile = profile,
            incomingServerSettings = incoming,
            outgoingServerSettings = outgoing,
            identities = listOf(Identity(name = "n", email = "user@example.com")),
        )
    }

    private fun createRemoteFolder(id: Long, type: FolderType, serverId: String): RemoteFolder {
        return RemoteFolder(id, serverId, "Folder $serverId", type)
    }

    private class FakeRemoteFolderQueryRepository : RemoteFolderQueryRepository {
        var remoteFolders: List<RemoteFolder> = emptyList()

        override suspend fun getAllByAccountId(accountId: AccountId): Outcome<List<RemoteFolder>, FolderError> =
            Outcome.success(remoteFolders)
    }

    private class FakeFolderDetailsRepository : FolderDetailsRepository {
        override suspend fun findById(accountId: AccountId, folderId: Long): Outcome<FolderDetails?, FolderError> =
            Outcome.success(null)

        override suspend fun update(accountId: AccountId, folderDetails: FolderDetails): Outcome<Unit, FolderError> =
            Outcome.success(Unit)

        override suspend fun update(
            accountId: AccountId,
            partialUpdate: PartialUpdatableFolderDetails,
        ): Outcome<Unit, FolderError> = Outcome.success(Unit)
    }

    private class FakeLegacyAccountManager(
        initialAccounts: List<LegacyAccount> = emptyList(),
    ) : LegacyAccountManager {
        private val accounts = initialAccounts.toMutableList()

        override fun getAll(): Flow<List<LegacyAccount>> = throw UnsupportedOperationException()
        override fun getById(id: AccountId): Flow<LegacyAccount?> = throw UnsupportedOperationException()
        override suspend fun update(account: LegacyAccount) = updateSync(account)
        override fun getByIdSync(id: AccountId): LegacyAccount? = accounts.find { it.id == id }
        override fun updateSync(account: LegacyAccount) {
            accounts.removeIf { it.id == account.id }
            accounts.add(account)
        }

        override fun getAccounts(): List<LegacyAccount> = accounts
        override fun getAccountsFlow(): Flow<List<LegacyAccount>> = throw UnsupportedOperationException()
        override fun getAccount(accountUuid: String): LegacyAccount? = accounts.find { it.uuid == accountUuid }
        override fun getAccountFlow(accountUuid: String): Flow<LegacyAccount?> = throw UnsupportedOperationException()
        override fun moveAccount(account: LegacyAccount, newPosition: Int) = Unit
        override fun saveAccount(account: LegacyAccount) = updateSync(account)
    }
}
