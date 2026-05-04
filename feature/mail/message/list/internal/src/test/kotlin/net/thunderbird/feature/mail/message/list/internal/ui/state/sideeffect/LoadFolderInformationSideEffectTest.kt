@file:Suppress("MaxLineLength")

package net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import app.k9mail.legacy.mailstore.FolderRepository
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.spy
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.common.state.sideeffect.StateSideEffectHandler
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.UnifiedAccountId
import net.thunderbird.feature.account.avatar.Avatar
import net.thunderbird.feature.account.profile.AccountProfile
import net.thunderbird.feature.account.profile.AccountProfileRepository
import net.thunderbird.feature.mail.folder.api.FolderType
import net.thunderbird.feature.mail.folder.api.RemoteFolder
import net.thunderbird.feature.mail.message.list.internal.fakes.FakeFolderRepository
import net.thunderbird.feature.mail.message.list.ui.event.FolderEvent
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.Account
import net.thunderbird.feature.mail.message.list.ui.state.Folder
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState
import net.thunderbird.feature.mail.folder.api.Folder as MailFolder

class LoadFolderInformationSideEffectTest : BaseSideEffectHandlerTest() {
    @Test
    fun `handle() should return Consumed when event is LoadConfigurations and folderId is set and accountIds size is one`() =
        runTest {
            // Arrange
            val accountId = AccountIdFactory.create()
            val folderId = 1L
            val folder = createMailFolder(id = folderId, name = "Inbox", isLocalOnly = true)
            val testSubject = createTestSubject(
                accountIds = setOf(accountId),
                folderId = folderId,
                folderRepository = createFolderRepository(
                    accountId = accountId,
                    folderId = folderId,
                    folder = folder,
                ),
            )

            // Act
            val result = testSubject.handle(
                event = MessageListEvent.LoadConfigurations,
                oldState = MessageListState.WarmingUp(),
                newState = createReadyWarmingUpState(),
            )

            // Assert
            assertThat(result).isEqualTo(StateSideEffectHandler.ConsumeResult.Consumed)
        }

    @Test
    fun `handle() should return Ignored when event is not LoadConfigurations`() = runTest {
        // Arrange
        val testSubject = createTestSubject(
            accountIds = setOf(AccountIdFactory.create()),
            folderId = 1L,
        )

        // Act
        val result = testSubject.handle(
            event = MessageListEvent.AllConfigsReady,
            oldState = MessageListState.WarmingUp(),
            newState = createReadyWarmingUpState(),
        )

        // Assert
        assertThat(result).isEqualTo(StateSideEffectHandler.ConsumeResult.Ignored)
    }

    @Test
    fun `handle() should return Consumed when folderId is null for unified folder`() = runTest {
        // Arrange
        val testSubject = createTestSubject(
            accountIds = setOf(AccountIdFactory.create()),
            folderId = null,
        )

        // Act
        val result = testSubject.handle(
            event = MessageListEvent.LoadConfigurations,
            oldState = MessageListState.WarmingUp(),
            newState = MessageListState.WarmingUp(),
        )

        // Assert
        assertThat(result).isEqualTo(StateSideEffectHandler.ConsumeResult.Consumed)
    }

    @Test
    fun `handle() should dispatch unified folder when folderId is null`() = runTest {
        // Arrange
        val dispatch = spy<suspend (MessageListEvent) -> Unit>(obj = {})
        val testSubject = createTestSubject(
            accountIds = setOf(AccountIdFactory.create()),
            folderId = null,
            dispatch = dispatch,
        )

        // Act
        testSubject.handle(
            event = MessageListEvent.LoadConfigurations,
            oldState = MessageListState.WarmingUp(),
            newState = MessageListState.WarmingUp(),
        )

        // Assert
        verifySuspend {
            dispatch(
                FolderEvent.FolderLoaded(
                    folder = Folder(
                        id = "unified_inbox",
                        account = Account(id = UnifiedAccountId, color = Color.Unspecified),
                        name = "Unified Inbox",
                        type = FolderType.INBOX,
                    ),
                ),
            )
        }
    }

    @Test
    fun `handle() should use first accountId when multiple accountIds provided`() = runTest {
        // Arrange
        val firstAccountId = AccountIdFactory.create()
        val secondAccountId = AccountIdFactory.create()
        val folderId = 1L
        val folder = createMailFolder(id = folderId, name = "Inbox", isLocalOnly = true)
        val expectedColor = Color.DarkGray
        val dispatch = spy<suspend (MessageListEvent) -> Unit>(obj = {})
        val testSubject = createTestSubject(
            accountIds = setOf(firstAccountId, secondAccountId),
            folderId = folderId,
            dispatch = dispatch,
            folderRepository = createFolderRepository(
                accountId = firstAccountId,
                folderId = folderId,
                folder = folder,
            ),
            profileRepository = FakeAccountProfileRepository(
                profiles = listOf(
                    createAccountProfile(accountId = firstAccountId, color = expectedColor),
                    createAccountProfile(accountId = secondAccountId),
                ),
            ),
        )

        // Act
        val result = testSubject.handle(
            event = MessageListEvent.LoadConfigurations,
            oldState = MessageListState.WarmingUp(),
            newState = MessageListState.WarmingUp(),
        )

        // Assert
        assertThat(result).isEqualTo(StateSideEffectHandler.ConsumeResult.Consumed)
        verifySuspend {
            dispatch(
                FolderEvent.FolderLoaded(
                    folder = Folder(
                        id = "local_folder",
                        account = Account(id = firstAccountId, color = expectedColor),
                        name = "Inbox",
                        type = FolderType.INBOX,
                    ),
                ),
            )
        }
    }

    @Test
    fun `handle() should dispatch FolderLoaded for local only folder`() = runTest {
        // Arrange
        val accountId = AccountIdFactory.create()
        val folderId = 7L
        val folder = createMailFolder(id = folderId, name = "Local", isLocalOnly = true)
        val dispatch = spy<suspend (MessageListEvent) -> Unit>(obj = {})
        val folderRepository = createFolderRepository(
            accountId = accountId,
            folderId = folderId,
            folder = folder,
        )
        val expectedColor = Color.Magenta
        val testSubject = createTestSubject(
            accountIds = setOf(accountId),
            folderId = folderId,
            dispatch = dispatch,
            folderRepository = folderRepository,
            profileRepository = FakeAccountProfileRepository(
                profiles = listOf(createAccountProfile(accountId = accountId, expectedColor)),
            ),
        )

        // Act
        testSubject.handle(
            event = MessageListEvent.LoadConfigurations,
            oldState = MessageListState.WarmingUp(),
            newState = MessageListState.WarmingUp(),
        )

        // Assert
        verifySuspend {
            dispatch(
                FolderEvent.FolderLoaded(
                    folder = Folder(
                        id = "local_folder",
                        account = Account(id = accountId, color = expectedColor),
                        name = "Local",
                        type = FolderType.INBOX,
                    ),
                ),
            )
        }
    }

    @Test
    fun `handle() should dispatch FolderLoaded for remote folder`() = runTest {
        // Arrange
        val accountId = AccountIdFactory.create()
        val folderId = 9L
        val folder = createMailFolder(id = folderId, name = "Remote", isLocalOnly = false)
        val remoteFolder = RemoteFolder(
            id = folderId,
            serverId = "server-id",
            name = "Remote",
            type = FolderType.INBOX,
        )
        val dispatch = spy<suspend (MessageListEvent) -> Unit>(obj = {})
        val folderRepository = createFolderRepository(
            accountId = accountId,
            folderId = folderId,
            folder = folder,
            remoteFolders = listOf(remoteFolder),
        )
        val expectedColor = Color.Blue
        val testSubject = createTestSubject(
            accountIds = setOf(accountId),
            folderId = folderId,
            dispatch = dispatch,
            folderRepository = folderRepository,
            profileRepository = FakeAccountProfileRepository(
                profiles = listOf(createAccountProfile(accountId = accountId, expectedColor)),
            ),
        )

        // Act
        testSubject.handle(
            event = MessageListEvent.LoadConfigurations,
            oldState = MessageListState.WarmingUp(),
            newState = MessageListState.WarmingUp(),
        )

        // Assert
        verifySuspend {
            dispatch(
                FolderEvent.FolderLoaded(
                    folder = Folder(
                        id = "server-id",
                        account = Account(id = accountId, color = expectedColor),
                        name = "Remote",
                        type = FolderType.INBOX,
                    ),
                ),
            )
        }
    }

    @Test
    fun `handle() should dispatch FolderLoaded with account color from profile`() = runTest {
        // Arrange
        val accountId = AccountIdFactory.create()
        val folderId = 5L
        val folder = createMailFolder(id = folderId, name = "Inbox", isLocalOnly = true)
        val expectedColor = 0xFF0000FF.toInt()
        val dispatch = spy<suspend (MessageListEvent) -> Unit>(obj = {})
        val testSubject = createTestSubject(
            accountIds = setOf(accountId),
            folderId = folderId,
            dispatch = dispatch,
            folderRepository = createFolderRepository(
                accountId = accountId,
                folderId = folderId,
                folder = folder,
            ),
            profileRepository = FakeAccountProfileRepository(
                profiles = listOf(
                    AccountProfile(
                        id = accountId,
                        name = "Test",
                        color = expectedColor,
                        avatar = Avatar.Monogram("T"),
                    ),
                ),
            ),
        )

        // Act
        testSubject.handle(
            event = MessageListEvent.LoadConfigurations,
            oldState = MessageListState.WarmingUp(),
            newState = MessageListState.WarmingUp(),
        )

        // Assert
        verifySuspend {
            dispatch(
                FolderEvent.FolderLoaded(
                    folder = Folder(
                        id = "local_folder",
                        account = Account(id = accountId, color = Color(expectedColor)),
                        name = "Inbox",
                        type = FolderType.INBOX,
                    ),
                ),
            )
        }
    }

    @Test
    fun `handle() should not dispatch when folder is null`() = runTest {
        // Arrange
        val accountId = AccountIdFactory.create()
        val folderId = 10L
        val dispatch = spy<suspend (MessageListEvent) -> Unit>(obj = {})
        val folderRepository = createFolderRepository(
            accountId = accountId,
            folderId = folderId,
            folder = null,
        )
        val testSubject = createTestSubject(
            accountIds = setOf(accountId),
            folderId = folderId,
            dispatch = dispatch,
            folderRepository = folderRepository,
        )

        // Act
        testSubject.handle(
            event = MessageListEvent.LoadConfigurations,
            oldState = MessageListState.WarmingUp(),
            newState = MessageListState.WarmingUp(),
        )

        // Assert
        verifySuspend(mode = VerifyMode.exactly(0)) { dispatch(any()) }
    }

    @Test
    fun `handle() should throw when remote folder is missing for non-local folder`() = runTest {
        // Arrange
        val accountId = AccountIdFactory.create()
        val folderId = 11L
        val folder = createMailFolder(id = folderId, name = "Remote", isLocalOnly = false)
        val remoteFolder = RemoteFolder(
            id = 999L,
            serverId = "other",
            name = "Other",
            type = FolderType.INBOX,
        )
        val folderRepository = createFolderRepository(
            accountId = accountId,
            folderId = folderId,
            folder = folder,
            remoteFolders = listOf(remoteFolder),
        )
        val testSubject = createTestSubject(
            accountIds = setOf(accountId),
            folderId = folderId,
            folderRepository = folderRepository,
        )

        // Act / Assert
        assertFailsWith<NoSuchElementException> {
            testSubject.handle(
                event = MessageListEvent.LoadConfigurations,
                oldState = MessageListState.WarmingUp(),
                newState = MessageListState.WarmingUp(),
            )
        }
    }

    @Test
    fun `factory should create LoadFolderInformationSideEffect`() {
        // Arrange
        val factory = LoadFolderInformationSideEffect.Factory(
            accountIds = setOf(AccountIdFactory.create()),
            folderId = 1L,
            logger = TestLogger(),
            folderRepository = createFolderRepository(
                accountId = AccountIdFactory.create(),
                folderId = 1L,
                folder = null,
            ),
            profileRepository = FakeAccountProfileRepository(),
        )

        // Act
        val result = factory.create(
            scope = mock(),
            dispatch = {},
            dispatchUiEffect = {},
        )

        // Assert
        assertThat(result).isInstanceOf(LoadFolderInformationSideEffect::class)
    }

    private fun createTestSubject(
        accountIds: Set<AccountId> = setOf(AccountIdFactory.create()),
        folderId: Long? = 1L,
        dispatch: suspend (MessageListEvent) -> Unit = {},
        logger: Logger = TestLogger(),
        folderRepository: FolderRepository = FakeFolderRepository(
            localFolders = emptyMap(),
            remoteFolders = emptyMap(),
        ),
        profileRepository: AccountProfileRepository = FakeAccountProfileRepository(
            profiles = accountIds.map(::createAccountProfile),
        ),
    ) = LoadFolderInformationSideEffect(
        accountIds = accountIds,
        folderId = folderId,
        dispatch = dispatch,
        logger = logger,
        folderRepository = folderRepository,
        profileRepository = profileRepository,
    )

    private fun createFolderRepository(
        accountId: AccountId,
        folderId: Long,
        folder: MailFolder?,
        remoteFolders: List<RemoteFolder> = emptyList(),
    ): FolderRepository = FakeFolderRepository(
        localFolders = mapOf(accountId to listOfNotNull(folder?.copy(id = folderId))),
        remoteFolders = mapOf(accountId to remoteFolders),
    )

    private fun createMailFolder(
        id: Long,
        name: String,
        isLocalOnly: Boolean,
        type: FolderType = FolderType.INBOX,
    ) = MailFolder(
        id = id,
        name = name,
        type = type,
        isLocalOnly = isLocalOnly,
    )

    private fun createAccountProfile(accountId: AccountId, color: Color = Color.Unspecified): AccountProfile =
        AccountProfile(
            id = accountId,
            name = "Test $accountId",
            color = color.toArgb(),
            avatar = Avatar.Monogram("T"),
        )

    private class FakeAccountProfileRepository(
        profiles: List<AccountProfile> = emptyList(),
    ) : AccountProfileRepository {
        private val profiles = flowOf(profiles)
        override fun getAll(): Flow<List<AccountProfile>> = profiles

        @OptIn(ExperimentalCoroutinesApi::class)
        override fun getById(id: AccountId): Flow<AccountProfile?> = profiles
            .flatMapConcat { it.asFlow() }
            .filter { it.id == id }

        override suspend fun update(accountProfile: AccountProfile) {
            error("Not implemented")
        }
    }
}
