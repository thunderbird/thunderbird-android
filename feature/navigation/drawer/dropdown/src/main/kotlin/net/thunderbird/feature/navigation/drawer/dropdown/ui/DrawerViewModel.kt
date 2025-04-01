package net.thunderbird.feature.navigation.drawer.dropdown.ui

import androidx.lifecycle.viewModelScope
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.thunderbird.feature.navigation.drawer.dropdown.domain.DomainContract.UseCase
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayAccount
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayAccountFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayUnifiedFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.TreeFolder
import net.thunderbird.feature.navigation.drawer.dropdown.ui.DrawerContract.Effect
import net.thunderbird.feature.navigation.drawer.dropdown.ui.DrawerContract.Event
import net.thunderbird.feature.navigation.drawer.dropdown.ui.DrawerContract.State
import net.thunderbird.feature.navigation.drawer.dropdown.ui.DrawerContract.ViewModel

/**
 * Delay before closing the drawer to avoid the drawer being closed immediately and give time
 * for the ripple effect to finish.
 */
private const val DRAWER_CLOSE_DELAY = 250L

@Suppress("MagicNumber", "TooManyFunctions")
internal class DrawerViewModel(
    private val getDrawerConfig: UseCase.GetDrawerConfig,
    private val saveDrawerConfig: UseCase.SaveDrawerConfig,
    private val getDisplayAccounts: UseCase.GetDisplayAccounts,
    private val getDisplayFoldersForAccount: UseCase.GetDisplayFoldersForAccount,
    private val getTreeFolders: UseCase.GetTreeFolders,
    private val syncAccount: UseCase.SyncAccount,
    private val syncAllAccounts: UseCase.SyncAllAccounts,
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
        val selectedAccount = accounts.find { it.id == state.value.selectedAccountId }
            ?: accounts.firstOrNull()

        updateState {
            it.copy(
                accounts = accounts.toImmutableList(),
                selectedAccountId = selectedAccount?.id,
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun loadFolders() {
        state.map {
            it.selectedAccountId?.let { accountId ->
                Pair(accountId, it.config.showUnifiedFolders)
            }
        }.filterNotNull()
            .distinctUntilChanged()
            .flatMapLatest { (accountId, showUnifiedInbox) ->
                getDisplayFoldersForAccount(accountId, showUnifiedInbox)
            }.collect { folders ->
                updateFolders(folders, getTreeFolders(folders, 3))
            }
    }

    private fun updateFolders(displayFolders: List<DisplayFolder>, rootFolder: TreeFolder) {
        val selectedFolder = displayFolders.find {
            it.id == state.value.selectedFolderId
        } ?: displayFolders.firstOrNull()

        updateState {
            it.copy(
                rootFolder = rootFolder,
                folders = displayFolders.toImmutableList(),
                selectedFolderId = selectedFolder?.id,
            )
        }
    }

    override fun event(event: Event) {
        when (event) {
            is Event.SelectAccount -> selectAccount(event.accountId)
            is Event.SelectFolder -> selectFolder(event.folderId)

            is Event.OnAccountClick -> openAccount(event.account)
            is Event.OnFolderClick -> openFolder(event.folder)
            is Event.OnAccountViewClick -> {
                openAccount(
                    state.value.accounts.nextOrFirst(event.account),
                )
            }

            Event.OnAccountSelectorClick -> {
                saveDrawerConfig(
                    state.value.config.copy(showAccountSelector = state.value.config.showAccountSelector.not()),
                ).launchIn(viewModelScope)
            }
            Event.OnManageFoldersClick -> emitEffect(Effect.OpenManageFolders)
            Event.OnSettingsClick -> emitEffect(Effect.OpenSettings)
            Event.OnSyncAccount -> onSyncAccount()
            Event.OnSyncAllAccounts -> onSyncAllAccounts()
        }
    }

    private fun selectAccount(accountId: String?) {
        updateState {
            it.copy(
                selectedAccountId = accountId,
            )
        }
    }

    private fun selectFolder(folderId: String?) {
        updateState {
            it.copy(
                selectedFolderId = folderId,
            )
        }
    }

    private fun openAccount(account: DisplayAccount?) {
        if (account != null) {
            emitEffect(Effect.OpenAccount(account.id))
        }
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

    private fun openFolder(folder: DisplayFolder) {
        if (folder is DisplayAccountFolder) {
            emitEffect(Effect.OpenFolder(folder.folder.id))
        } else if (folder is DisplayUnifiedFolder) {
            emitEffect(Effect.OpenUnifiedFolder)
        }

        viewModelScope.launch {
            delay(DRAWER_CLOSE_DELAY)
            emitEffect(Effect.CloseDrawer)
        }
    }

    private fun onSyncAccount() {
        if (state.value.isLoading || state.value.selectedAccountId == null) return

        viewModelScope.launch {
            updateState {
                it.copy(isLoading = true)
            }

            state.value.selectedAccountId?.let { syncAccount(it).collect() }

            updateState {
                it.copy(isLoading = false)
            }
        }
    }

    private fun onSyncAllAccounts() {
        if (state.value.isLoading) return

        viewModelScope.launch {
            updateState {
                it.copy(isLoading = true)
            }

            syncAllAccounts().collect()

            updateState {
                it.copy(isLoading = false)
            }
        }
    }
}
