package app.k9mail.feature.navigation.drawer.ui

import androidx.lifecycle.viewModelScope
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.navigation.drawer.domain.DomainContract.UseCase
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayAccount
import app.k9mail.feature.navigation.drawer.ui.DrawerContract.Effect
import app.k9mail.feature.navigation.drawer.ui.DrawerContract.Event
import app.k9mail.feature.navigation.drawer.ui.DrawerContract.State
import app.k9mail.feature.navigation.drawer.ui.DrawerContract.ViewModel
import app.k9mail.legacy.ui.folder.DisplayFolder
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

@Suppress("MagicNumber")
class DrawerViewModel(
    private val getDrawerConfig: UseCase.GetDrawerConfig,
    private val getDisplayAccounts: UseCase.GetDisplayAccounts,
    private val getDisplayFoldersForAccount: UseCase.GetDisplayFoldersForAccount,
    private val syncMail: UseCase.SyncMail,
    initialState: State = State(),
) : BaseViewModel<State, Event, Effect>(
    initialState = initialState,
),
    ViewModel {

    init {
        viewModelScope.launch {
            getDrawerConfig().collectLatest { config ->
                updateState {
                    it.copy(config = config)
                }
            }
        }

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

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun loadFolders() {
        state.mapNotNull { it.currentAccount?.account?.uuid }
            .distinctUntilChanged()
            .flatMapLatest { accountUuid ->
                getDisplayFoldersForAccount(accountUuid)
            }.collectLatest { folders ->
                updateState {
                    it.copy(folders = folders.toImmutableList())
                }
            }
    }

    override fun event(event: Event) {
        when (event) {
            Event.OnRefresh -> refresh()
            is Event.OnAccountClick -> selectAccount(event.account)
            is Event.OnFolderClick -> selectFolder(event.folder)
            is Event.OnAccountViewClick -> {
                selectAccount(
                    state.value.accounts.nextOrFirst(event.account)!!,
                )
            }
        }
    }

    private fun selectAccount(account: DisplayAccount) {
        viewModelScope.launch {
            updateState {
                it.copy(
                    currentAccount = account,
                )
            }
        }

        emitEffect(Effect.OpenAccount(account.account))
    }

    private fun ImmutableList<DisplayAccount>.nextOrFirst(account: DisplayAccount): DisplayAccount? {
        val index = indexOf(account)
        return if (index == -1) {
            null
        } else if (index == size - 1) {
            get(0)
        } else {
            get(index + 1)
        }
    }

    private fun selectFolder(folder: DisplayFolder) {
        updateState {
            it.copy(selectedFolder = folder)
        }
        emitEffect(Effect.OpenFolder(folder.folder.id))

        viewModelScope.launch {
            delay(DRAWER_CLOSE_DELAY)
            emitEffect(Effect.CloseDrawer)
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            updateState {
                it.copy(isLoading = true)
            }

            syncMail(state.value.currentAccount?.account).collect {
                // nothing to do
            }

            updateState {
                it.copy(isLoading = false)
            }
        }
    }
}

/**
 * Delay before closing the drawer to avoid the drawer being closed immediately and give time
 * for the ripple effect to finish.
 */
private const val DRAWER_CLOSE_DELAY = 250L
