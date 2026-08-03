package net.thunderbird.feature.mail.message.list.internal.domain.usecase

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasMessage
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.mail.folders.FolderServerId
import kotlin.test.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.thunderbird.backend.api.folder.RemoteFolderCreationOutcome
import net.thunderbird.backend.api.folder.RemoteFolderCreator
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.mail.folder.api.FolderType
import net.thunderbird.feature.mail.folder.api.SpecialFolderSelection
import net.thunderbird.feature.mail.message.list.domain.CreateArchiveFolderOutcome
import net.thunderbird.feature.mail.message.list.internal.fakes.FakeBackendFolderUpdater
import net.thunderbird.feature.mail.message.list.internal.fakes.FakeBackendStorageFactory
import net.thunderbird.feature.mail.message.list.internal.fakes.FakeLegacyAccount
import net.thunderbird.feature.mail.message.list.internal.fakes.FakeLegacyAccountManager
import net.thunderbird.feature.mail.message.list.internal.fakes.FakeSpecialFolderUpdaterFactory
import com.fsck.k9.mail.FolderType as LegacyFolderType

@OptIn(ExperimentalUuidApi::class)
@Suppress("MaxLineLength")
class CreateArchiveFolderTest {
    @Test
    fun `invoke should emit InvalidFolderName and complete flow when folderName is invalid`() = runTest {
        // Arrange
        val accountUuid = Uuid.random().toHexString()
        val accounts = createAccountList(accountUuid = accountUuid)
        val accountManager = FakeLegacyAccountManager(accounts)
        val testSubject = createTestSubject(accountManager = accountManager)
        val folderName = ""

        // Act
        testSubject(AccountIdFactory.of(accountUuid), folderName).test {
            // Assert
            val outcome = awaitItem()
            assertThat(outcome)
                .isInstanceOf<Outcome.Failure<CreateArchiveFolderOutcome.Error>>()
                .prop("error") { it.error }
                .isInstanceOf<CreateArchiveFolderOutcome.Error.InvalidFolderName>()
                .prop("folderName") { it.folderName }
                .isEqualTo(folderName)

            assertThat(accountManager.getByIdCalls).isEmpty()

            awaitComplete()
        }
    }

    @Test
    fun `invoke should emit AccountNotFound and complete flow when no account uuid matches with account list`() =
        runTest {
            // Arrange
            val accountUuid = Uuid.random().toHexString()
            val accounts = createAccountList()
            val accountManager = FakeLegacyAccountManager(accounts)
            val testSubject = createTestSubject(accountManager = accountManager)
            val folderName = "TheFolder"

            // Act
            testSubject(AccountIdFactory.of(accountUuid), folderName).test {
                // Assert
                val outcome = awaitItem()
                assertThat(outcome)
                    .isInstanceOf<Outcome.Failure<CreateArchiveFolderOutcome.Error>>()
                    .prop("error") { it.error }
                    .isEqualTo(CreateArchiveFolderOutcome.Error.AccountNotFound)

                assertThat(accountManager.getByIdCalls).containsExactly(AccountIdFactory.of(accountUuid))
                awaitComplete()
            }
        }

    @Test
    fun `invoke should emit UnhandledError and complete flow when BackendStorage createFolder throws MessagingException`() =
        runTest {
            // Arrange
            val accountUuid = Uuid.random().toHexString()
            val accounts = createAccountList(accountUuid)
            val exception = MessagingException("this is an error")
            val backendFolderUpdater = FakeBackendFolderUpdater(exception)
            val remoteFolderCreatorFactory = FakeRemoteFolderCreatorFactory(outcome = null)
            val testSubject = createTestSubject(
                accounts = accounts,
                backendStorageFactory = FakeBackendStorageFactory(backendFolderUpdater),
                remoteFolderCreatorFactory = remoteFolderCreatorFactory,
            )
            val folderName = "TheFolder"

            // Act
            testSubject(AccountIdFactory.of(accountUuid), folderName).test {
                // Assert
                val outcome = awaitItem()
                assertThat(outcome)
                    .isInstanceOf<Outcome.Failure<CreateArchiveFolderOutcome.Error>>()
                    .prop("error") { it.error }
                    .isInstanceOf<CreateArchiveFolderOutcome.Error.UnhandledError>()
                    .prop("throwable") { it.throwable }
                    .hasMessage(exception.message)

                assertThat(remoteFolderCreatorFactory.createCalls).isEmpty()

                awaitComplete()
            }
        }

    @Test
    fun `invoke should emit LocalFolderCreationError and complete flow when BackendStorage createFolder returns null`() =
        runTest {
            // Arrange
            val accountUuid = Uuid.random().toHexString()
            val accounts = createAccountList(accountUuid)
            val backendStorageFactory = FakeBackendStorageFactory(
                FakeBackendFolderUpdater(
                    returnEmptySetWhenCreatingFolders = true,
                ),
            )
            val remoteFolderCreatorFactory = FakeRemoteFolderCreatorFactory(outcome = null)
            val testSubject = createTestSubject(
                accounts = accounts,
                backendStorageFactory = backendStorageFactory,
            )
            val folderName = "TheFolder"

            // Act
            testSubject(AccountIdFactory.of(accountUuid), folderName).test {
                // Assert
                val outcome = awaitItem()
                assertThat(outcome)
                    .isInstanceOf<Outcome.Failure<CreateArchiveFolderOutcome.Error>>()
                    .prop("error") { it.error }
                    .isInstanceOf<CreateArchiveFolderOutcome.Error.LocalFolderCreationError>()
                    .prop("folderName") { it.folderName }
                    .isEqualTo(folderName)

                assertThat(backendStorageFactory.backendFolderUpdater.createFoldersCalls)
                    .containsExactly(createExpectedFolderInfo(folderName))
                assertThat(remoteFolderCreatorFactory.createCalls).isEmpty()
                awaitComplete()
            }
        }

    @Test
    fun `invoke should emit LocalFolderCreated when BackendStorage createFolder returns folderId`() = runTest {
        // Arrange
        val accountUuid = Uuid.random().toHexString()
        val accounts = createAccountList(accountUuid = accountUuid)
        val backendStorageFactory = FakeBackendStorageFactory(
            FakeBackendFolderUpdater(),
        )
        val testSubject = createTestSubject(
            accounts = accounts,
            remoteFolderCreatorOutcome = Outcome.success(RemoteFolderCreationOutcome.Success.Created),
            backendStorageFactory = backendStorageFactory,
        )
        val folderName = "TheFolder"

        // Act
        testSubject(AccountIdFactory.of(accountUuid), folderName).test {
            // Assert
            val outcome = awaitItem()
            assertThat(outcome)
                .isInstanceOf<Outcome.Success<CreateArchiveFolderOutcome.Success>>()
                .prop("data") { it.data }
                .isEqualTo(CreateArchiveFolderOutcome.Success.LocalFolderCreated)

            assertThat(backendStorageFactory.backendFolderUpdater.createFoldersCalls)
                .containsExactly(createExpectedFolderInfo(folderName))

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `invoke should emit SyncStarted when local folder synchronization with remote starts`() = runTest {
        // Arrange
        val accountUuid = Uuid.random().toHexString()
        val accounts = createAccountList(accountUuid)
        val backendStorageFactory = FakeBackendStorageFactory(
            FakeBackendFolderUpdater(),
        )
        val testSubject = createTestSubject(
            accounts = accounts,
            remoteFolderCreatorOutcome = Outcome.success(RemoteFolderCreationOutcome.Success.Created),
            backendStorageFactory = backendStorageFactory,
        )
        val folderName = "TheFolder"

        // Act
        testSubject(AccountIdFactory.of(accountUuid), folderName).test {
            // Assert
            skipItems(count = 1) // Skip LocalFolderCreated event.
            val outcome = awaitItem()
            assertThat(outcome)
                .isInstanceOf<Outcome.Success<CreateArchiveFolderOutcome.Success>>()
                .prop("data") { it.data }
                .isInstanceOf<CreateArchiveFolderOutcome.Success.SyncStarted>()
                .prop("serverId") { it.serverId }
                .isEqualTo(FolderServerId(folderName))

            assertThat(backendStorageFactory.backendFolderUpdater.createFoldersCalls)
                .containsExactly(createExpectedFolderInfo(folderName))

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `invoke should emit SyncError when remote folder creation fails for any reason`() = runTest {
        // Arrange
        val accountUuid = Uuid.random().toHexString()
        val accounts = createAccountList(accountUuid)
        val backendStorageFactory = FakeBackendStorageFactory(
            FakeBackendFolderUpdater(),
        )
        val error = RemoteFolderCreationOutcome.Error.AlreadyExists
        val testSubject = createTestSubject(
            accounts = accounts,
            remoteFolderCreatorOutcome = Outcome.failure(error),
            backendStorageFactory = backendStorageFactory,
        )
        val folderName = "TheFolder"

        // Act
        testSubject(AccountIdFactory.of(accountUuid), folderName).test {
            // Assert
            skipItems(count = 2) // Skip LocalFolderCreated and SyncStarted event.
            val outcome = awaitItem()
            assertThat(outcome)
                .isInstanceOf<Outcome.Failure<CreateArchiveFolderOutcome.Error>>()
                .prop("error") { it.error }
                .isInstanceOf<CreateArchiveFolderOutcome.Error.SyncError.Failed>()
                .isEqualTo(
                    CreateArchiveFolderOutcome.Error.SyncError.Failed(
                        serverId = FolderServerId(folderName),
                        message = error.toString(),
                        exception = null,
                    ),
                )

            assertThat(backendStorageFactory.backendFolderUpdater.createFoldersCalls)
                .containsExactly(createExpectedFolderInfo(folderName))

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @Suppress("LongMethod")
    fun `invoke should emit Success when local and remote folder creation succeed`() = runTest {
        // Arrange
        val accountUuid = Uuid.random().toHexString()
        val accounts = createAccountList(accountUuid)
        val accountManager = FakeLegacyAccountManager(accounts)
        val backendStorageFactory = FakeBackendStorageFactory(
            FakeBackendFolderUpdater(),
        )
        val specialFolderUpdaterFactory = FakeSpecialFolderUpdaterFactory()
        val remoteFolderCreatorFactory = FakeRemoteFolderCreatorFactory(
            Outcome.success(RemoteFolderCreationOutcome.Success.Created),
        )
        val testSubject = createTestSubject(
            accountManager = accountManager,
            remoteFolderCreatorFactory = remoteFolderCreatorFactory,
            backendStorageFactory = backendStorageFactory,
            specialFolderUpdaterFactory = specialFolderUpdaterFactory,
        )
        val folderName = "TheFolder"

        // Act
        testSubject(AccountIdFactory.of(accountUuid), folderName).test {
            // Assert
            skipItems(count = 2) // Skip LocalFolderCreated and SyncStarted event.
            var outcome = awaitItem()
            assertThat(outcome)
                .isInstanceOf<Outcome.Success<CreateArchiveFolderOutcome.Success>>()
                .prop("data") { it.data }
                .isEqualTo(CreateArchiveFolderOutcome.Success.UpdatingSpecialFolders)

            outcome = awaitItem()
            assertThat(outcome)
                .isInstanceOf<Outcome.Success<CreateArchiveFolderOutcome.Success>>()
                .prop("data") { it.data }
                .isEqualTo(CreateArchiveFolderOutcome.Success.Created)

            assertThat(accountManager.getByIdCalls).containsExactly(AccountIdFactory.of(accountUuid))
            assertThat(backendStorageFactory.backendFolderUpdater.createFoldersCalls)
                .containsExactly(createExpectedFolderInfo(folderName))

            assertThat(remoteFolderCreatorFactory.instance.createCalls)
                .containsExactly(
                    FakeRemoteFolderCreatorFactory.CreateCall(
                        folderServerId = FolderServerId(folderName),
                        mustCreate = false,
                        folderType = LegacyFolderType.ARCHIVE,
                    ),
                )

            assertThat(specialFolderUpdaterFactory.specialFolderUpdater.setSpecialFolderCalls)
                .transform { calls -> calls.map { it.type to it.selection } }
                .containsExactly(FolderType.ARCHIVE to SpecialFolderSelection.MANUAL)

            assertThat(specialFolderUpdaterFactory.specialFolderUpdater.updateSpecialFoldersCalls).isEqualTo(1)

            assertThat(accountManager.savedAccounts.map { it.id }).containsExactly(AccountIdFactory.of(accountUuid))

            awaitComplete()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun createTestSubject(
        accounts: List<LegacyAccount> = emptyList(),
        accountManager: FakeLegacyAccountManager = FakeLegacyAccountManager(accounts),
        backendStorageFactory: FakeBackendStorageFactory = FakeBackendStorageFactory(),
        remoteFolderCreatorOutcome: Outcome<
            RemoteFolderCreationOutcome.Success,
            RemoteFolderCreationOutcome.Error,
            >? = null,
        remoteFolderCreatorFactory: FakeRemoteFolderCreatorFactory = FakeRemoteFolderCreatorFactory(
            outcome = remoteFolderCreatorOutcome,
        ),
        specialFolderUpdaterFactory: FakeSpecialFolderUpdaterFactory = FakeSpecialFolderUpdaterFactory(),
    ): CreateArchiveFolder =
        CreateArchiveFolder(
            accountManager = accountManager,
            backendStorageFactory = backendStorageFactory,
            remoteFolderCreatorFactory = remoteFolderCreatorFactory,
            specialFolderUpdaterFactory = specialFolderUpdaterFactory,
            ioDispatcher = UnconfinedTestDispatcher(),
        )

    private fun createAccountList(
        accountUuid: String = Uuid.random().toHexString(),
        size: Int = 10,
    ) = List(size = size) {
        val id = if (it == 0) AccountIdFactory.of(accountUuid) else AccountIdFactory.create()
        FakeLegacyAccount(id = id)
    }

    private fun createExpectedFolderInfo(folderName: String) = listOf(
        FolderInfo(
            serverId = folderName,
            name = folderName,
            type = LegacyFolderType.ARCHIVE,
        ),
    )
}

private class FakeRemoteFolderCreatorFactory(
    private val outcome: Outcome<RemoteFolderCreationOutcome.Success, RemoteFolderCreationOutcome.Error>?,
) : RemoteFolderCreator.Factory {
    val createCalls = mutableListOf<AccountId>()
    val instance = FakeRemoteFolderCreator()

    override suspend fun create(accountId: AccountId): RemoteFolderCreator {
        createCalls += accountId
        return instance
    }

    data class CreateCall(
        val folderServerId: FolderServerId,
        val mustCreate: Boolean,
        val folderType: LegacyFolderType,
    )

    inner class FakeRemoteFolderCreator : RemoteFolderCreator {
        val createCalls = mutableListOf<CreateCall>()

        override suspend fun create(
            folderServerId: FolderServerId,
            mustCreate: Boolean,
            folderType: LegacyFolderType,
        ): Outcome<RemoteFolderCreationOutcome.Success, RemoteFolderCreationOutcome.Error> {
            createCalls += CreateCall(
                folderServerId = folderServerId,
                mustCreate = mustCreate,
                folderType = folderType,
            )
            return outcome ?: error("Not expected to be called in this context.")
        }
    }
}
