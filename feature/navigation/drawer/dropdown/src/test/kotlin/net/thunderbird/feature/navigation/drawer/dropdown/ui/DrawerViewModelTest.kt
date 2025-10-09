package net.thunderbird.feature.navigation.drawer.dropdown.ui

import app.k9mail.core.ui.compose.testing.mvi.advanceUntilIdle
import app.k9mail.core.ui.compose.testing.mvi.assertThatAndEffectTurbineConsumed
import app.k9mail.core.ui.compose.testing.mvi.runMviTest
import app.k9mail.core.ui.compose.testing.mvi.turbinesWithInitialStateCheck
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.testing.coroutines.MainDispatcherRule
import net.thunderbird.feature.mail.folder.api.Folder
import net.thunderbird.feature.mail.folder.api.FolderType
import net.thunderbird.feature.navigation.drawer.api.NavigationDrawerExternalContract.DrawerConfig
import net.thunderbird.feature.navigation.drawer.dropdown.domain.DomainContract.UseCase
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayTreeFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.MailDisplayAccount
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.MailDisplayFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.UnifiedDisplayFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.UnifiedDisplayFolderType
import net.thunderbird.feature.navigation.drawer.dropdown.ui.DrawerContract.Effect
import net.thunderbird.feature.navigation.drawer.dropdown.ui.DrawerContract.Event
import net.thunderbird.feature.navigation.drawer.dropdown.ui.DrawerContract.State
import net.thunderbird.feature.navigation.drawer.dropdown.ui.FakeData.MAIL_DISPLAY_ACCOUNT
import org.junit.Rule
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
internal class DrawerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `should collect drawer config`() = runTest {
        val drawerConfig = createDrawerConfig()
        val getDrawerConfigFlow = MutableStateFlow(drawerConfig)
        val testSubject = createTestSubject(
            drawerConfigFlow = getDrawerConfigFlow,
        )

        advanceUntilIdle()

        assertThat(testSubject.state.value.config).isEqualTo(drawerConfig)

        val newDrawerConfig = createDrawerConfig(showUnifiedInbox = true)

        getDrawerConfigFlow.emit(newDrawerConfig)

        advanceUntilIdle()

        assertThat(testSubject.state.value.config).isEqualTo(newDrawerConfig)
    }

    @Test
    fun `should change loading state when OnSyncAccount event is received`() = runMviTest {
        val initialState = State(
            accounts = listOf(MAIL_DISPLAY_ACCOUNT).toImmutableList(),
            selectedAccountId = MAIL_DISPLAY_ACCOUNT.id,
        )
        val testSubject = createTestSubject(
            initialState = initialState,
            displayAccountsFlow = flow { emit(listOf(MAIL_DISPLAY_ACCOUNT)) },
            syncAccountFlow = flow {
                delay(25)
                emit(Result.success(Unit))
            },
        )
        val turbines = turbinesWithInitialStateCheck(testSubject, initialState)

        testSubject.event(Event.OnSyncAccount)

        assertThat(turbines.stateTurbine.awaitItem()).isEqualTo(initialState.copy(isLoading = true))
        assertThat(turbines.stateTurbine.awaitItem()).isEqualTo(initialState.copy(isLoading = false))
    }

    @Test
    fun `should skip loading when no account is selected and OnSyncAccount event is received`() = runMviTest {
        val initialState = State(selectedAccountId = null)
        var counter = 0
        val testSubject = createTestSubject(
            initialState = initialState,
            syncAccountFlow = flow {
                delay(25)
                counter++
                emit(Result.success(Unit))
            },
        )

        val turbines = turbinesWithInitialStateCheck(testSubject, initialState)

        testSubject.event(Event.OnSyncAccount)

        advanceUntilIdle()

        assertThat(testSubject.state.value.isLoading).isEqualTo(false)
        assertThat(counter).isEqualTo(0)

        turbines.stateTurbine.ensureAllEventsConsumed()
        turbines.effectTurbine.ensureAllEventsConsumed()
    }

    @Test
    fun `should skip loading when already loading and OnSyncAccount event received`() = runMviTest {
        val initialState = State(isLoading = true)
        var counter = 0
        val testSubject = createTestSubject(
            initialState = initialState,
            syncAccountFlow = flow {
                counter++
                delay(25)
                emit(Result.success(Unit))
            },
        )
        val turbines = turbinesWithInitialStateCheck(testSubject, initialState)

        testSubject.event(Event.OnSyncAccount)

        advanceUntilIdle()

        assertThat(testSubject.state.value.isLoading).isEqualTo(true)
        assertThat(counter).isEqualTo(0)

        turbines.stateTurbine.ensureAllEventsConsumed()
        turbines.effectTurbine.ensureAllEventsConsumed()
    }

    @Test
    fun `should change loading state when OnSyncAllAccounts event is received`() = runMviTest {
        val testSubject = createTestSubject(
            syncAllAccounts = flow {
                delay(25)
                emit(Result.success(Unit))
            },
        )
        val initialState = State(isLoading = false)
        val turbines = turbinesWithInitialStateCheck(testSubject, initialState)

        testSubject.event(Event.OnSyncAllAccounts)

        assertThat(turbines.stateTurbine.awaitItem()).isEqualTo(State(isLoading = true))
        assertThat(turbines.stateTurbine.awaitItem()).isEqualTo(State(isLoading = false))
    }

    @Test
    fun `should skip loading when already loading and OnSyncAllAccounts event received`() = runMviTest {
        val initialState = State(isLoading = true)
        var counter = 0
        val testSubject = createTestSubject(
            initialState = initialState,
            syncAccountFlow = flow {
                counter++
                delay(25)
                emit(Result.success(Unit))
            },
        )
        val turbines = turbinesWithInitialStateCheck(testSubject, initialState)

        testSubject.event(Event.OnSyncAllAccounts)

        advanceUntilIdle()

        assertThat(testSubject.state.value.isLoading).isEqualTo(true)
        assertThat(counter).isEqualTo(0)

        turbines.stateTurbine.ensureAllEventsConsumed()
        turbines.effectTurbine.ensureAllEventsConsumed()
    }

    @Test
    fun `should collect display accounts when created and select first as selected`() = runTest {
        val displayAccounts = createDisplayAccountList(2)
        val getDisplayAccountsFlow = MutableStateFlow(displayAccounts)
        val testSubject = createTestSubject(
            displayAccountsFlow = getDisplayAccountsFlow,
        )

        advanceUntilIdle()

        assertThat(testSubject.state.value.accounts.size).isEqualTo(displayAccounts.size)
        assertThat(testSubject.state.value.accounts).isEqualTo(displayAccounts)
        assertThat(testSubject.state.value.selectedAccountId).isEqualTo(displayAccounts.first().id)
    }

    @Test
    fun `should reselect selected account when old not present anymore`() = runTest {
        val displayAccounts = createDisplayAccountList(3)
        val getDisplayAccountsFlow = MutableStateFlow(displayAccounts)
        val testSubject = createTestSubject(
            displayAccountsFlow = getDisplayAccountsFlow,
        )

        advanceUntilIdle()

        val newDisplayAccounts = displayAccounts.drop(1)
        getDisplayAccountsFlow.emit(newDisplayAccounts)

        advanceUntilIdle()

        assertThat(testSubject.state.value.accounts.size).isEqualTo(newDisplayAccounts.size)
        assertThat(testSubject.state.value.accounts).isEqualTo(newDisplayAccounts)
        assertThat(testSubject.state.value.selectedAccountId).isEqualTo(newDisplayAccounts.first().id)
    }

    @Test
    fun `should set selected account to null when no accounts are present`() = runTest {
        val getDisplayAccountsFlow = MutableStateFlow(emptyList<MailDisplayAccount>())
        val testSubject = createTestSubject(
            displayAccountsFlow = getDisplayAccountsFlow,
        )

        advanceUntilIdle()

        assertThat(testSubject.state.value.accounts.size).isEqualTo(0)
        assertThat(testSubject.state.value.selectedAccountId).isEqualTo(null)
    }

    @Test
    fun `should send OpenAccount effect when OnAccountClick event is received`() = runMviTest {
        val displayAccounts = createDisplayAccountList(3)
        val getDisplayAccountsFlow = MutableStateFlow(displayAccounts)
        val testSubject = createTestSubject(
            displayAccountsFlow = getDisplayAccountsFlow,
        )
        val turbines = turbinesWithInitialStateCheck(
            testSubject,
            State(
                accounts = displayAccounts.toImmutableList(),
                selectedAccountId = displayAccounts.first().id,
            ),
        )

        advanceUntilIdle()

        testSubject.event(Event.OnAccountClick(displayAccounts[1]))

        advanceUntilIdle()

        turbines.assertThatAndEffectTurbineConsumed {
            isEqualTo(Effect.OpenAccount(displayAccounts[1].id))
        }
    }

    @Test
    fun `should collect display folders for selected account`() = runTest {
        val displayAccounts = createDisplayAccountList(3)
        val getDisplayAccountsFlow = MutableStateFlow(displayAccounts)
        val displayFoldersMap = mapOf(
            displayAccounts[0].id to createDisplayFolderList(3),
        )
        val displayFoldersFlow = MutableStateFlow(displayFoldersMap)
        val testSubject = createTestSubject(
            displayAccountsFlow = getDisplayAccountsFlow,
            displayFoldersFlow = displayFoldersFlow,
        )

        advanceUntilIdle()

        val displayFolders = displayFoldersMap[displayAccounts[0].id] ?: emptyList()
        assertThat(testSubject.state.value.folders.size).isEqualTo(displayFolders.size)
        assertThat(testSubject.state.value.folders).isEqualTo(displayFolders)
    }

    @Test
    fun `should collect display folders when selected account is changed`() = runTest {
        val displayAccounts = createDisplayAccountList(3)
        val getDisplayAccountsFlow = MutableStateFlow(displayAccounts)
        val displayFoldersMap = mapOf(
            displayAccounts[0].id to createDisplayFolderList(1),
            displayAccounts[1].id to createDisplayFolderList(5),
            displayAccounts[2].id to createDisplayFolderList(10),
        )
        val displayFoldersFlow = MutableStateFlow(displayFoldersMap)
        val testSubject = createTestSubject(
            displayAccountsFlow = getDisplayAccountsFlow,
            displayFoldersFlow = displayFoldersFlow,
        )

        advanceUntilIdle()

        testSubject.event(Event.SelectAccount(displayAccounts[1].id))

        advanceUntilIdle()

        val displayFolders = displayFoldersMap[displayAccounts[1].id] ?: emptyList()
        assertThat(testSubject.state.value.folders.size).isEqualTo(displayFolders.size)
        assertThat(testSubject.state.value.folders).isEqualTo(displayFolders)
    }

    @Test
    fun `should emit OpenFolder effect when OnFolderClick event is received`() = runMviTest {
        val displayAccounts = createDisplayAccountList(3)
        val getDisplayAccountsFlow = MutableStateFlow(displayAccounts)
        val displayFoldersMap = mapOf(
            displayAccounts[0].id to createDisplayFolderList(3),
        )
        val displayFoldersFlow = MutableStateFlow(displayFoldersMap)
        val initialState = State(
            accounts = displayAccounts.toImmutableList(),
            selectedAccountId = displayAccounts[0].id,
            folders = displayFoldersMap[displayAccounts[0].id]!!.toImmutableList(),
            selectedFolderId = displayFoldersMap[displayAccounts[0].id]!![0].id,
            selectedFolder = displayFoldersMap[displayAccounts[0].id]!![0],
        )
        val testSubject = createTestSubject(
            displayAccountsFlow = getDisplayAccountsFlow,
            displayFoldersFlow = displayFoldersFlow,
        )
        val turbines = turbinesWithInitialStateCheck(testSubject, initialState)

        advanceUntilIdle()

        val displayFolders = displayFoldersMap[displayAccounts[0].id] ?: emptyList()
        testSubject.event(Event.OnFolderClick(displayFolders[1]))

        // Consume the state update
        turbines.awaitStateItem()

        assertThat(turbines.awaitEffectItem()).isEqualTo(
            Effect.OpenFolder(
                accountId = requireNotNull(displayFolders[1].accountId),
                folderId = displayFolders[1].folder.id,
            ),
        )

        turbines.assertThatAndEffectTurbineConsumed {
            isEqualTo(Effect.CloseDrawer)
        }
    }

    @Test
    fun `should emit OpenUnifiedFolder when OnFolderClick event for unified folder is received`() =
        runMviTest {
            val displayAccounts = createDisplayAccountList(1)
            val getDisplayAccountsFlow = MutableStateFlow(displayAccounts)
            val displayFoldersMap = mapOf(
                displayAccounts[0].id to
                    createDisplayFolderList(1) + listOf(createDisplayUnifiedFolder()),
            )
            val displayFoldersFlow = MutableStateFlow(displayFoldersMap)
            val initialState = State(
                accounts = displayAccounts.toImmutableList(),
                selectedAccountId = displayAccounts[0].id,
                folders = displayFoldersMap[displayAccounts[0].id]!!.toImmutableList(),
                selectedFolderId = displayFoldersMap[displayAccounts[0].id]!![0].id,
                selectedFolder = displayFoldersMap[displayAccounts[0].id]!![0],
            )
            val testSubject = createTestSubject(
                displayAccountsFlow = getDisplayAccountsFlow,
                displayFoldersFlow = displayFoldersFlow,
            )
            val turbines = turbinesWithInitialStateCheck(testSubject, initialState)

            advanceUntilIdle()

            val displayFolders = displayFoldersMap[displayAccounts[0].id] ?: emptyList()
            testSubject.event(Event.OnFolderClick(displayFolders[1]))

            // Consume the state update
            turbines.awaitStateItem()

            assertThat(turbines.awaitEffectItem()).isEqualTo(Effect.OpenUnifiedFolder)

            turbines.assertThatAndEffectTurbineConsumed {
                isEqualTo(Effect.CloseDrawer)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `when initial state has drawerConfigWithAccountSelectorDisabled saveDrawerConfig should receive drawerConfigWithAccountSelectorEnabled  when OnAccountSelectorClick event is received`() =
        runTest {
            val drawerConfigWithAccountSelectorEnabled = createDrawerConfig(showAccountSelector = true)
            val drawerConfigWithAccountSelectorDisabled = createDrawerConfig(showAccountSelector = false)

            val saveDrawerConfig: UseCase.SaveDrawerConfig = mock()
            whenever(
                saveDrawerConfig.invoke(any<DrawerConfig>()),
            ).thenReturn(flowOf(Unit))

            val testSubject = createTestSubject(
                initialState = State(config = drawerConfigWithAccountSelectorDisabled, showAccountSelection = false),
                saveDrawerConfig = saveDrawerConfig,
                drawerConfigFlow = flowOf(drawerConfigWithAccountSelectorDisabled),
            )

            val captor = argumentCaptor<DrawerConfig>()

            testSubject.event(Event.OnAccountSelectorClick)
            advanceUntilIdle()
            verify(saveDrawerConfig, times(1)).invoke(captor.capture())
            assertThat(captor.firstValue).isEqualTo(drawerConfigWithAccountSelectorEnabled)

            // Verify that showAccountSelection is toggled after the delay
            assertThat(testSubject.state.value.showAccountSelection).isEqualTo(true)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `when initial state has drawerConfigWithAccountSelectorEnabled saveDrawerConfig should receive drawerConfigWithAccountSelectorDisabled  when OnAccountSelectorClick event is received`() =
        runTest {
            val drawerConfigWithAccountSelectorEnabled = createDrawerConfig(showAccountSelector = true)
            val drawerConfigWithAccountSelectorDisabled = createDrawerConfig(showAccountSelector = false)

            val saveDrawerConfig: UseCase.SaveDrawerConfig = mock()
            whenever(
                saveDrawerConfig.invoke(any<DrawerConfig>()),
            ).thenReturn(flowOf(Unit))

            val testSubject = createTestSubject(
                initialState = State(config = drawerConfigWithAccountSelectorEnabled, showAccountSelection = false),
                saveDrawerConfig = saveDrawerConfig,
                drawerConfigFlow = flowOf(drawerConfigWithAccountSelectorEnabled),
            )

            val captor = argumentCaptor<DrawerConfig>()

            testSubject.event(Event.OnAccountSelectorClick)
            advanceUntilIdle()
            verify(saveDrawerConfig, times(1)).invoke(captor.capture())
            assertThat(captor.firstValue).isEqualTo(drawerConfigWithAccountSelectorDisabled)

            // Verify that showAccountSelection is toggled after the delay
            assertThat(testSubject.state.value.showAccountSelection).isEqualTo(true)
        }

    @Test
    fun `should emit OpenManageFolders effect when OnManageFoldersClick event is received`() = runMviTest {
        val testSubject = createTestSubject()
        val turbines = turbinesWithInitialStateCheck(testSubject, State())

        testSubject.event(Event.OnManageFoldersClick)

        turbines.assertThatAndEffectTurbineConsumed {
            isEqualTo(Effect.OpenManageFolders)
        }
    }

    @Test
    fun `should emit OpenSettings effect when OnSettingsClick event is received`() = runMviTest {
        val testSubject = createTestSubject()
        val turbines = turbinesWithInitialStateCheck(testSubject, State())

        testSubject.event(Event.OnSettingsClick)

        turbines.assertThatAndEffectTurbineConsumed {
            isEqualTo(Effect.OpenSettings)
        }
    }

    private fun createTestSubject(
        initialState: State = State(),
        drawerConfigFlow: Flow<DrawerConfig> = flow { emit(createDrawerConfig()) },
        displayAccountsFlow: Flow<List<MailDisplayAccount>> = flow { emit(emptyList()) },
        displayFoldersFlow: Flow<Map<String, List<DisplayFolder>>> = flow { emit(emptyMap()) },
        displayTreeFolder: DisplayTreeFolder = DisplayTreeFolder(
            displayFolder = null,
            displayName = null,
            totalUnreadCount = 0,
            totalStarredCount = 0,
            children = persistentListOf(),
        ),
        syncAccountFlow: Flow<Result<Unit>> = flow { emit(Result.success(Unit)) },
        syncAllAccounts: Flow<Result<Unit>> = flow { emit(Result.success(Unit)) },
        saveDrawerConfig: UseCase.SaveDrawerConfig = mock(),
    ): DrawerViewModel {
        return DrawerViewModel(
            initialState = initialState,
            getDrawerConfig = { drawerConfigFlow },
            getDisplayAccounts = { displayAccountsFlow },
            getDisplayFoldersForAccount = { accountId ->
                displayFoldersFlow.map { it[accountId] ?: emptyList() }
            },
            getDisplayTreeFolder = { folders, maxDepth ->
                displayTreeFolder
            },
            syncAccount = { syncAccountFlow },
            syncAllAccounts = { syncAllAccounts },
            saveDrawerConfig = saveDrawerConfig,
        )
    }

    private fun createDrawerConfig(
        showUnifiedInbox: Boolean = false,
        showStarredCount: Boolean = false,
        showAccountSelector: Boolean = true,
    ): DrawerConfig {
        return DrawerConfig(
            showUnifiedFolders = showUnifiedInbox,
            showStarredCount = showStarredCount,
            showAccountSelector = showAccountSelector,
        )
    }

    private fun createDisplayAccount(
        id: String = "uuid",
        name: String = "name",
        email: String = "test@example.com",
        unreadCount: Int = 0,
        starredCount: Int = 0,
    ): MailDisplayAccount {
        return MailDisplayAccount(
            id = id,
            name = name,
            email = email,
            color = 0,
            unreadMessageCount = unreadCount,
            starredMessageCount = starredCount,
        )
    }

    private fun createDisplayAccountList(count: Int): List<MailDisplayAccount> {
        return List(count) { index ->
            createDisplayAccount(
                id = "uuid-$index",
            )
        }
    }

    private fun createDisplayFolder(
        accountId: String = "uuid",
        id: Long = 1234,
        name: String = "name",
        type: FolderType = FolderType.REGULAR,
        unreadCount: Int = 0,
        starredCount: Int = 0,
    ): MailDisplayFolder {
        val folder = Folder(
            id = id,
            name = name,
            type = type,
            isLocalOnly = false,
        )

        return MailDisplayFolder(
            accountId = accountId,
            folder = folder,
            isInTopGroup = false,
            unreadMessageCount = unreadCount,
            starredMessageCount = starredCount,
            pathDelimiter = "/",
        )
    }

    private fun createDisplayFolderList(count: Int): List<MailDisplayFolder> {
        return List(count) { index ->
            createDisplayFolder(
                id = index.toLong() + 100,
            )
        }
    }

    private fun createDisplayUnifiedFolder(
        id: String = "unified_inbox",
        unifiedType: UnifiedDisplayFolderType = UnifiedDisplayFolderType.INBOX,
        unreadCount: Int = 0,
        starredCount: Int = 0,
    ): UnifiedDisplayFolder {
        return UnifiedDisplayFolder(
            id = id,
            unifiedType = unifiedType,
            unreadMessageCount = unreadCount,
            starredMessageCount = starredCount,
        )
    }
}
