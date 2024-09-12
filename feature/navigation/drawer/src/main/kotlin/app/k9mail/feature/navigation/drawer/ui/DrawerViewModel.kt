package app.k9mail.feature.navigation.drawer.ui

import androidx.lifecycle.viewModelScope
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.navigation.drawer.domain.DomainContract.UseCase
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayAccount
import app.k9mail.feature.navigation.drawer.ui.DrawerContract.Effect
import app.k9mail.feature.navigation.drawer.ui.DrawerContract.Event
import app.k9mail.feature.navigation.drawer.ui.DrawerContract.State
import app.k9mail.feature.navigation.drawer.ui.DrawerContract.ViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Suppress("MagicNumber")
class DrawerViewModel(
    private val getDisplayAccounts: UseCase.GetDisplayAccounts,
    private val getDisplayFoldersForAccount: UseCase.GetDisplayFoldersForAccount,
    initialState: State = State(),
) : BaseViewModel<State, Event, Effect>(
    initialState = initialState,
),
    ViewModel {

    init {
        viewModelScope.launch {
            loadAccounts()
        }

        viewModelScope.launch {
            loadFolders()
        }
    }

    private suspend fun loadAccounts() {
        getDisplayAccounts().collectLatest { accounts ->
            updateAccounts(accounts)
        }
    }

    private fun updateAccounts(accounts: List<DisplayAccount>) {
        val currentAccountUuid = state.value.currentAccount?.account?.uuid
        val isCurrentAccountAvailable = accounts.any { currentAccountUuid == it.account.uuid }

        updateState {
            if (isCurrentAccountAvailable) {
                it.copy(accounts = accounts.toImmutableList())
            } else {
                it.copy(
                    accounts = accounts.toImmutableList(),
                    currentAccount = accounts.firstOrNull(),
                )
            }
        }
    }

    private suspend fun loadFolders() {
        state.map { it.currentAccount }
            .distinctUntilChanged()
            .collectLatest { currentAccount ->
                if (currentAccount != null) {
                    getDisplayFoldersForAccount(currentAccount.account.uuid).collectLatest { folders ->
                        updateState {
                            it.copy(folders = folders.toImmutableList())
                        }
                    }
                }
            }
    }

    override fun event(event: Event) {
        when (event) {
            Event.OnRefresh -> refresh()
        }
    }

    private fun refresh() {
        if (state.value.isLoading) {
            return
        }
        viewModelScope.launch {
            updateState {
                it.copy(isLoading = true)
            }

            // TODO: replace with actual data loading
            delay(500)

            updateState {
                it.copy(isLoading = false)
            }
        }
    }
}
