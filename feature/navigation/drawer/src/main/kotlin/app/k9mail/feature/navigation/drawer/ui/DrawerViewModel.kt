package app.k9mail.feature.navigation.drawer.ui

import androidx.lifecycle.viewModelScope
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.navigation.drawer.domain.DomainContract.UseCase
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayAccount
import app.k9mail.feature.navigation.drawer.ui.DrawerContract.Effect
import app.k9mail.feature.navigation.drawer.ui.DrawerContract.Event
import app.k9mail.feature.navigation.drawer.ui.DrawerContract.State
import app.k9mail.feature.navigation.drawer.ui.DrawerContract.ViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Suppress("MagicNumber")
class DrawerViewModel(
    private val getDisplayAccounts: UseCase.GetDisplayAccounts,
    initialState: State = State(),
) : BaseViewModel<State, Event, Effect>(
    initialState = initialState,
),
    ViewModel {

    init {
        viewModelScope.launch {
            getDisplayAccounts().collect { accounts -> updateAccounts(accounts) }
        }
    }

    private fun updateAccounts(accounts: List<DisplayAccount>) {
        val currentAccountUuid = state.value.currentAccount?.account?.uuid
        val isCurrentAccountAvailable = accounts.any { currentAccountUuid == it.account.uuid }

        updateState {
            if (isCurrentAccountAvailable) {
                it.copy(accounts = accounts)
            } else {
                it.copy(
                    currentAccount = accounts.firstOrNull(),
                    accounts = accounts,
                )
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
