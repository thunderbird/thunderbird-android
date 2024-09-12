package app.k9mail.feature.navigation.drawer.ui

import app.k9mail.core.ui.compose.testing.MainDispatcherRule
import app.k9mail.core.ui.compose.testing.mvi.eventStateTest
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayAccount
import app.k9mail.feature.navigation.drawer.ui.DrawerContract.Event
import app.k9mail.feature.navigation.drawer.ui.DrawerContract.State
import app.k9mail.legacy.account.Account
import app.k9mail.legacy.account.Identity
import app.k9mail.legacy.ui.folder.DisplayFolder
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
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
            getDisplayAccountsFlow = getDisplayAccountsFlow,
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
            getDisplayAccountsFlow = getDisplayAccountsFlow,
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
            getDisplayAccountsFlow = getDisplayAccountsFlow,
        )

        advanceUntilIdle()

        assertThat(testSubject.state.value.accounts.size).isEqualTo(0)
        assertThat(testSubject.state.value.currentAccount).isEqualTo(null)
    }

    private fun createTestSubject(
        getDisplayAccountsFlow: Flow<List<DisplayAccount>> = flow { emit(emptyList()) },
        getDisplayFoldersForAccount: Flow<List<DisplayFolder>> = flow { emit(emptyList()) },
    ): DrawerViewModel {
        return DrawerViewModel(
            getDisplayAccounts = { getDisplayAccountsFlow },
            getDisplayFoldersForAccount = { getDisplayFoldersForAccount },
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
}
