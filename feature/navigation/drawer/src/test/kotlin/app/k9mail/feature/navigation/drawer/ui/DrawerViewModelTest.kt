package app.k9mail.feature.navigation.drawer.ui

import app.k9mail.core.mail.folder.api.Folder
import app.k9mail.core.mail.folder.api.FolderType
import app.k9mail.core.ui.compose.testing.MainDispatcherRule
import app.k9mail.core.ui.compose.testing.mvi.assertThatAndEffectTurbineConsumed
import app.k9mail.core.ui.compose.testing.mvi.eventStateTest
import app.k9mail.core.ui.compose.testing.mvi.turbines
import app.k9mail.core.ui.compose.testing.mvi.turbinesWithInitialStateCheck
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayAccount
import app.k9mail.feature.navigation.drawer.ui.DrawerContract.Effect
import app.k9mail.feature.navigation.drawer.ui.DrawerContract.Event
import app.k9mail.feature.navigation.drawer.ui.DrawerContract.State
import app.k9mail.legacy.account.Account
import app.k9mail.legacy.account.Identity
import app.k9mail.legacy.ui.folder.DisplayFolder
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
class DrawerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `should change loading state when OnRefresh event is received`() = runTest {
        val testSubject = createTestSubject()

        eventStateTest(
            viewModel = testSubject,
            initialState = State(isLoading = false),
            event = Event.OnRefresh,
            expectedState = State(isLoading = true),
            coroutineScope = backgroundScope,
        )

        advanceUntilIdle()

        assertThat(testSubject.state.value.isLoading).isEqualTo(false)
    }

    @Test
    fun `should collect display accounts when created and select first as current`() = runTest {
        val displayAccounts = createDisplayAccountList(3)
        val getDisplayAccountsFlow = MutableStateFlow(displayAccounts)
        val testSubject = createTestSubject(
            displayAccountsFlow = getDisplayAccountsFlow,
        )

        advanceUntilIdle()

        assertThat(testSubject.state.value.accounts.size).isEqualTo(displayAccounts.size)
        assertThat(testSubject.state.value.accounts).isEqualTo(displayAccounts)
        assertThat(testSubject.state.value.currentAccount).isEqualTo(displayAccounts.first())
    }

    @Test
    fun `should reselect current account when old not present anymore`() = runTest {
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
        assertThat(testSubject.state.value.currentAccount).isEqualTo(newDisplayAccounts.first())
    }

    @Test
    fun `should set current account to null when no accounts are present`() = runTest {
        val getDisplayAccountsFlow = MutableStateFlow(emptyList<DisplayAccount>())
        val testSubject = createTestSubject(
            displayAccountsFlow = getDisplayAccountsFlow,
        )

        advanceUntilIdle()

        assertThat(testSubject.state.value.accounts.size).isEqualTo(0)
        assertThat(testSubject.state.value.currentAccount).isEqualTo(null)
    }

    @Test
    fun `should set current account when OnAccountClick event is received`() = runTest {
        val displayAccounts = createDisplayAccountList(3)
        val getDisplayAccountsFlow = MutableStateFlow(displayAccounts)
        val testSubject = createTestSubject(
            displayAccountsFlow = getDisplayAccountsFlow,
        )

        advanceUntilIdle()

        testSubject.event(Event.OnAccountClick(displayAccounts[1]))

        advanceUntilIdle()

        assertThat(testSubject.state.value.currentAccount).isEqualTo(displayAccounts[1])
    }

    @Test
    fun `should collect display folders for current account`() = runTest {
        val displayAccounts = createDisplayAccountList(3)
        val getDisplayAccountsFlow = MutableStateFlow(displayAccounts)
        val displayFoldersMap = mapOf(
            displayAccounts[0].account.uuid to createDisplayFolderList(3),
        )
        val testSubject = createTestSubject(
            displayAccountsFlow = getDisplayAccountsFlow,
            displayFoldersMap = displayFoldersMap,
        )

        advanceUntilIdle()

        val displayFolders = displayFoldersMap[displayAccounts[0].account.uuid] ?: emptyList()
        assertThat(testSubject.state.value.folders.size).isEqualTo(displayFolders.size)
        assertThat(testSubject.state.value.folders).isEqualTo(displayFolders)
    }

    @Test
    fun `should collect display folders when current account is changed`() = runTest {
        val displayAccounts = createDisplayAccountList(3)
        val getDisplayAccountsFlow = MutableStateFlow(displayAccounts)
        val displayFoldersMap = mapOf(
            displayAccounts[0].account.uuid to createDisplayFolderList(1),
            displayAccounts[1].account.uuid to createDisplayFolderList(5),
            displayAccounts[2].account.uuid to createDisplayFolderList(10),
        )
        val testSubject = createTestSubject(
            displayAccountsFlow = getDisplayAccountsFlow,
            displayFoldersMap = displayFoldersMap,
        )

        advanceUntilIdle()

        testSubject.event(Event.OnAccountClick(displayAccounts[1]))

        advanceUntilIdle()

        val displayFolders = displayFoldersMap[displayAccounts[1].account.uuid] ?: emptyList()
        assertThat(testSubject.state.value.folders.size).isEqualTo(displayFolders.size)
        assertThat(testSubject.state.value.folders).isEqualTo(displayFolders)
    }

    @Test
    fun `should set selected folder when OnFolderClick event is received`() = runTest {
        val displayAccounts = createDisplayAccountList(3)
        val getDisplayAccountsFlow = MutableStateFlow(displayAccounts)
        val displayFoldersMap = mapOf(
            displayAccounts[0].account.uuid to createDisplayFolderList(3),
        )
        val initialState = State(
            accounts = displayAccounts.toImmutableList(),
            currentAccount = displayAccounts[0],
            folders = displayFoldersMap[displayAccounts[0].account.uuid]?.toImmutableList() ?: persistentListOf(),
        )
        val testSubject = createTestSubject(
            displayAccountsFlow = getDisplayAccountsFlow,
            displayFoldersMap = displayFoldersMap,
        )
        val turbines = turbinesWithInitialStateCheck(testSubject, initialState)

        advanceUntilIdle()

        val displayFolders = displayFoldersMap[displayAccounts[0].account.uuid] ?: emptyList()
        testSubject.event(Event.OnFolderClick(displayFolders[1]))

        assertThat(turbines.awaitStateItem().selectedFolder).isEqualTo(displayFolders[1])

        assertThat(turbines.awaitEffectItem()).isEqualTo(Effect.OpenFolder(displayFolders[1].folder.id))

        turbines.assertThatAndEffectTurbineConsumed {
            isEqualTo(Effect.CloseDrawer)
        }
    }

    private fun createTestSubject(
        displayAccountsFlow: Flow<List<DisplayAccount>> = flow { emit(emptyList()) },
        displayFoldersMap: Map<String, List<DisplayFolder>> = emptyMap(),
    ): DrawerViewModel {
        return DrawerViewModel(
            getDisplayAccounts = { displayAccountsFlow },
            getDisplayFoldersForAccount = { accountUuid ->
                flow { emit(displayFoldersMap[accountUuid] ?: emptyList()) }
            },
        )
    }

    private fun createDisplayAccount(
        uuid: String = "uuid",
        name: String = "name",
        email: String = "test@example.com",
        unreadCount: Int = 0,
        starredCount: Int = 0,
    ): DisplayAccount {
        val account = Account(
            uuid = uuid,
        ).also {
            it.identities = ArrayList()

            val identity = Identity(
                signatureUse = false,
                signature = "",
                description = "",
            )
            it.identities.add(identity)

            it.name = name
            it.email = email
        }

        return DisplayAccount(
            account = account,
            unreadMessageCount = unreadCount,
            starredMessageCount = starredCount,
        )
    }

    private fun createDisplayAccountList(count: Int): List<DisplayAccount> {
        return List(count) { index ->
            createDisplayAccount(
                uuid = "uuid-$index",
            )
        }
    }

    private fun createDisplayFolder(
        id: Long = 1234,
        name: String = "name",
        type: FolderType = FolderType.REGULAR,
        unreadCount: Int = 0,
        starredCount: Int = 0,
    ): DisplayFolder {
        val folder = Folder(
            id = id,
            name = name,
            type = type,
            isLocalOnly = false,
        )

        return DisplayFolder(
            folder = folder,
            isInTopGroup = false,
            unreadMessageCount = unreadCount,
            starredMessageCount = starredCount,
        )
    }

    private fun createDisplayFolderList(count: Int): List<DisplayFolder> {
        return List(count) { index ->
            createDisplayFolder(
                id = index.toLong() + 100,
            )
        }
    }
}
