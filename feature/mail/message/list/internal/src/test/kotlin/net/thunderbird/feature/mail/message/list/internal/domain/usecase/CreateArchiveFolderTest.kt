package net.thunderbird.feature.mail.message.list.internal.domain.usecase

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.mail.folders.FolderServerId
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.spy
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode.Companion.exactly
import dev.mokkery.verifySuspend
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
        val accountManager = spy(FakeLegacyAccountManager(accounts))
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

            verify(exactly(0)) { accountManager.getById(id = any()) }

            awaitComplete()
        }
    }

    @Test
    fun `invoke should emit AccountNotFound and complete flow when no account uuid matches with account list`() =
        runTest {
            // Arrange
            val accountUuid = Uuid.random().toHexString()
            val accounts = createAccountList()
            val accountManager = spy(FakeLegacyAccountManager(accounts))
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

                verify(exactly(1)) { accountManager.getById(AccountIdFactory.of(accountUuid)) }
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
            val remoteFolderCreatorFactory = spy(FakeRemoteFolderCreatorFactory(outcome = null))
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

                verify(exactly(0)) { remoteFolderCreatorFactory.create(accountId = any()) }

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
            val remoteFolderCreatorFactory = spy(FakeRemoteFolderCreatorFactory(outcome = null))
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

                verify(exactly(1)) {
                    // verify doesn't support verifying the extension function `createFolder`,
                    // thus we verify the call of `createFolders(list)` instead.
                    backendStorageFactory.backendFolderUpdater.createFolders(
                        eq(
                            listOf(
                                FolderInfo(
                                    serverId = folderName,
                                    name = folderName,
                                    type = LegacyFolderType.ARCHIVE,
                                ),
                            ),
                        ),
                    )
                }
                verify(exactly(0)) { remoteFolderCreatorFactory.create(accountId = any()) }
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

            verify(exactly(1)) {
                // verify doesn't support verifying the extension function `createFolder`,
                // thus we verify the call of `createFolders(list)` instead.
                backendStorageFactory.backendFolderUpdater.createFolders(
                    eq(
                        listOf(
                            FolderInfo(
                                serverId = folderName,
                                name = folderName,
                                type = LegacyFolderType.ARCHIVE,
                            ),
                        ),
                    ),
                )
            }

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

            verify(exactly(1)) {
                // verify doesn't support verifying the extension function `createFolder`,
                // thus we verify the call of `createFolders(list)` instead.
                backendStorageFactory.backendFolderUpdater.createFolders(
                    eq(
                        listOf(
                            FolderInfo(
                                serverId = folderName,
                                name = folderName,
                                type = LegacyFolderType.ARCHIVE,
                            ),
                        ),
                    ),
                )
            }

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

            verify(exactly(1)) {
                // verify doesn't support verifying the extension function `createFolder`,
                // thus we verify the call of `createFolders(list)` instead.
                backendStorageFactory.backendFolderUpdater.createFolders(
                    eq(
                        listOf(
                            FolderInfo(
                                serverId = folderName,
                                name = folderName,
                                type = LegacyFolderType.ARCHIVE,
                            ),
                        ),
                    ),
                )
            }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @Suppress("LongMethod")
    fun `invoke should emit Success when local and remote folder creation succeed`() = runTest {
        // Arrange
        val accountUuid = Uuid.random().toHexString()
        val accounts = createAccountList(accountUuid)
        val accountManager = spy(FakeLegacyAccountManager(accounts))
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

            verify(exactly(1)) { accountManager.getById(AccountIdFactory.of(accountUuid)) }
            verify(exactly(1)) {
                // verify doesn't support verifying the extension function `createFolder`,
                // thus we verify the call of `createFolders(list)` instead.
                backendStorageFactory.backendFolderUpdater.createFolders(
                    eq(
                        listOf(
                            FolderInfo(
                                serverId = folderName,
                                name = folderName,
                                type = LegacyFolderType.ARCHIVE,
                            ),
                        ),
                    ),
                )
            }

            verifySuspend(exactly(1)) {
                remoteFolderCreatorFactory.instance.create(
                    folderServerId = FolderServerId(folderName),
                    mustCreate = false,
                    folderType = LegacyFolderType.ARCHIVE,
                )
            }

            verify(exactly(1)) {
                specialFolderUpdaterFactory.specialFolderUpdater.setSpecialFolder(
                    type = FolderType.ARCHIVE,
                    folderId = any(),
                    selection = SpecialFolderSelection.MANUAL,
                )
            }

            verify(exactly(1)) {
                specialFolderUpdaterFactory.specialFolderUpdater.updateSpecialFolders()
            }

            verify(exactly(1)) {
                accountManager.saveAccount(account = any())
            }

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
}

private open class FakeRemoteFolderCreatorFactory(
    protected open val outcome: Outcome<RemoteFolderCreationOutcome.Success, RemoteFolderCreationOutcome.Error>?,
) : RemoteFolderCreator.Factory {
    open var instance: RemoteFolderCreator = spy<RemoteFolderCreator>(FakeRemoteFolderCreator())
        protected set

    override fun create(accountId: AccountId): RemoteFolderCreator = instance

    private open inner class FakeRemoteFolderCreator : RemoteFolderCreator {
        override suspend fun create(
            folderServerId: FolderServerId,
            mustCreate: Boolean,
            folderType: LegacyFolderType,
        ): Outcome<RemoteFolderCreationOutcome.Success, RemoteFolderCreationOutcome.Error> =
            outcome ?: error("Not expected to be called in this context.")
    }
}
