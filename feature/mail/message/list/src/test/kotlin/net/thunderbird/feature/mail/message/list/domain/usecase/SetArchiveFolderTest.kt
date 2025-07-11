package net.thunderbird.feature.mail.message.list.domain.usecase

import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import com.fsck.k9.mail.MessagingException
import dev.mokkery.matcher.any
import dev.mokkery.matcher.matching
import dev.mokkery.spy
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode.Companion.exactly
import kotlin.random.Random
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.mail.account.api.BaseAccount
import net.thunderbird.feature.mail.folder.api.FolderType
import net.thunderbird.feature.mail.folder.api.RemoteFolder
import net.thunderbird.feature.mail.folder.api.SpecialFolderSelection
import net.thunderbird.feature.mail.message.list.domain.SetAccountFolderOutcome
import net.thunderbird.feature.mail.message.list.fakes.FakeAccount
import net.thunderbird.feature.mail.message.list.fakes.FakeAccountManager
import net.thunderbird.feature.mail.message.list.fakes.FakeBackendFolderUpdater
import net.thunderbird.feature.mail.message.list.fakes.FakeBackendStorageFactory
import net.thunderbird.feature.mail.message.list.fakes.FakeSpecialFolderUpdaterFactory
import org.junit.Test
import com.fsck.k9.mail.FolderType as LegacyFolderType

@OptIn(ExperimentalUuidApi::class)
@Suppress("MaxLineLength")
class SetArchiveFolderTest {
    @Test
    fun `invoke should successfully create folder and update account when given valid input`() = runTest {
        // Arrange
        val accountUuid = Uuid.random().toHexString()
        val accounts = listOf(FakeAccount(uuid = accountUuid))

        val fakeBackendStorageFactory = FakeBackendStorageFactory()
        val fakeAccountManager = spy(FakeAccountManager(accounts))
        val fakeSpecialFolderUpdaterFactory = FakeSpecialFolderUpdaterFactory()
        val testSubject =
            createTestSubject(fakeAccountManager, fakeBackendStorageFactory, fakeSpecialFolderUpdaterFactory)
        val folder = createRemoteFolder()

        // Act
        val outcome = testSubject(accountUuid, folder)

        // Assert
        assertThat(outcome)
            .isInstanceOf<Outcome.Success<SetAccountFolderOutcome.Success>>()
            .prop(name = "data") { it.data }
            .isEqualTo(SetAccountFolderOutcome.Success)

        verify(exactly(1)) {
            fakeBackendStorageFactory.backendFolderUpdater.changeFolder(
                folderServerId = folder.serverId,
                name = folder.name,
                type = LegacyFolderType.ARCHIVE,
            )
        }
        verify(exactly(1)) { fakeBackendStorageFactory.backendFolderUpdater.close() }
        verify(exactly(1)) {
            fakeSpecialFolderUpdaterFactory.specialFolderUpdater.setSpecialFolder(
                type = FolderType.ARCHIVE,
                folderId = folder.id,
                selection = SpecialFolderSelection.MANUAL,
            )
        }
        verify(exactly(1)) {
            fakeSpecialFolderUpdaterFactory.specialFolderUpdater.updateSpecialFolders()
        }
        verify(exactly(1)) {
            fakeAccountManager.saveAccount(
                account = matching {
                    it.uuid == accountUuid
                },
            )
        }
    }

    @Test
    fun `invoke should return AccountNotFound when account is not found`() = runTest {
        // Arrange
        val accounts = listOf<FakeAccount>()
        val testSubject = createTestSubject(accounts)
        val accountUuid = Uuid.random().toHexString()
        val folder = createRemoteFolder()

        // Act
        val outcome = testSubject(accountUuid, folder)

        // Assert
        assertThat(outcome)
            .isInstanceOf<Outcome.Failure<SetAccountFolderOutcome.Error>>()
            .prop(name = "error") { it.error }
            .isEqualTo(SetAccountFolderOutcome.Error.AccountNotFound)
    }

    @Test
    fun `invoke should return UnhandledError when changeFolder throws MessagingException`() = runTest {
        // Arrange
        val accountUuid = Uuid.random().toHexString()
        val accounts = listOf(FakeAccount(uuid = accountUuid))

        val exception = MessagingException("this is an error")
        val fakeBackendStorageFactory = FakeBackendStorageFactory(
            backendFolderUpdater = FakeBackendFolderUpdater(exception = exception),
        )
        val fakeAccountManager = spy(FakeAccountManager(accounts))
        val fakeSpecialFolderUpdaterFactory = FakeSpecialFolderUpdaterFactory()
        val testSubject =
            createTestSubject(fakeAccountManager, fakeBackendStorageFactory, fakeSpecialFolderUpdaterFactory)
        val folder = createRemoteFolder()

        // Act
        val outcome = testSubject(accountUuid, folder)

        // Assert
        assertThat(outcome)
            .isInstanceOf<Outcome.Failure<SetAccountFolderOutcome.Error>>()
            .prop(name = "error") { it.error }
            .isInstanceOf<SetAccountFolderOutcome.Error.UnhandledError>()
            .prop("throwable") { it.throwable }
            .hasMessage(exception.message)

        verify(exactly(1)) {
            fakeBackendStorageFactory.backendFolderUpdater.changeFolder(
                folderServerId = folder.serverId,
                name = folder.name,
                type = LegacyFolderType.ARCHIVE,
            )
        }

        verify(exactly(1)) { fakeBackendStorageFactory.backendFolderUpdater.close() }

        verify(exactly(0)) {
            fakeSpecialFolderUpdaterFactory.specialFolderUpdater.setSpecialFolder(
                type = any(),
                folderId = any(),
                selection = any(),
            )
        }
        verify(exactly(0)) {
            fakeSpecialFolderUpdaterFactory.specialFolderUpdater.updateSpecialFolders()
        }
        verify(exactly(0)) {
            fakeAccountManager.saveAccount(account = any())
        }
    }

    private fun createTestSubject(
        accounts: List<BaseAccount>,
        backendStorageFactory: FakeBackendStorageFactory = FakeBackendStorageFactory(),
        specialFolderUpdaterFactory: FakeSpecialFolderUpdaterFactory = FakeSpecialFolderUpdaterFactory(),
    ): SetArchiveFolder = createTestSubject(
        accountManager = FakeAccountManager(accounts),
        backendStorageFactory = backendStorageFactory,
        specialFolderUpdaterFactory = specialFolderUpdaterFactory,
    )

    private fun createTestSubject(
        accountManager: FakeAccountManager,
        backendStorageFactory: FakeBackendStorageFactory = FakeBackendStorageFactory(),
        specialFolderUpdaterFactory: FakeSpecialFolderUpdaterFactory = FakeSpecialFolderUpdaterFactory(),
    ): SetArchiveFolder {
        return SetArchiveFolder(
            accountManager = accountManager,
            backendStorageFactory = backendStorageFactory,
            specialFolderUpdaterFactory = specialFolderUpdaterFactory,
        )
    }

    private fun createRemoteFolder(
        id: Long = Random.nextLong(),
        serverId: String = "remote_folder_$id",
        name: String = serverId,
    ): RemoteFolder = RemoteFolder(
        id = id,
        serverId = serverId,
        name = name,
        type = FolderType.ARCHIVE,
    )
}
