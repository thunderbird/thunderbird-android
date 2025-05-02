package net.thunderbird.feature.navigation.drawer.siderail.ui

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
import net.thunderbird.feature.navigation.drawer.siderail.domain.DomainContract
import net.thunderbird.feature.navigation.drawer.siderail.domain.entity.DisplayAccount
import net.thunderbird.feature.navigation.drawer.siderail.domain.entity.DisplayAccountFolder
import net.thunderbird.feature.navigation.drawer.siderail.domain.entity.DisplayFolder
import net.thunderbird.feature.navigation.drawer.siderail.domain.entity.DisplayUnifiedFolder

@Suppress("MagicNumber", "TooManyFunctions")
internal class DrawerViewModel(
    private val getDrawerConfig: DomainContract.UseCase.GetDrawerConfig,
    private val saveDrawerConfig: DomainContract.UseCase.SaveDrawerConfig,
    private val getDisplayAccounts: DomainContract.UseCase.GetDisplayAccounts,
    private val getDisplayFoldersForAccount: DomainContract.UseCase.GetDisplayFoldersForAccount,
    private val syncAccount: DomainContract.UseCase.SyncAccount,
    private val syncAllAccounts: DomainContract.UseCase.SyncAllAccounts,
    initialState: DrawerContract.State = DrawerContract.State(),
) : BaseViewModel<DrawerContract.State, DrawerContract.Event, DrawerContract.Effect>(
    initialState = initialState,
),
    DrawerContract.ViewModel {

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
                updateFolders(folders)
            }
    }

    private fun updateFolders(displayFolders: List<DisplayFolder>) {
        val selectedFolder = displayFolders.find {
            it.id == state.value.selectedFolderId
        } ?: displayFolders.firstOrNull()

        updateState {
            it.copy(
                folders = displayFolders.toImmutableList(),
                selectedFolderId = selectedFolder?.id,
            )
        }
    }

    override fun event(event: DrawerContract.Event) {
        when (event) {
            is DrawerContract.Event.SelectAccount -> selectAccount(event.accountId)
            is DrawerContract.Event.SelectFolder -> selectFolder(event.folderId)

            is DrawerContract.Event.OnAccountClick -> openAccount(event.account)
            is DrawerContract.Event.OnFolderClick -> openFolder(event.folder)
            is DrawerContract.Event.OnAccountViewClick -> {
                openAccount(
                    state.value.accounts.nextOrFirst(event.account),
                )
            }

            DrawerContract.Event.OnAccountSelectorClick -> {
                saveDrawerConfig(
                    state.value.config.copy(showAccountSelector = state.value.config.showAccountSelector.not()),
                ).launchIn(viewModelScope)
            }
            DrawerContract.Event.OnManageFoldersClick -> emitEffect(DrawerContract.Effect.OpenManageFolders)
            DrawerContract.Event.OnSettingsClick -> emitEffect(DrawerContract.Effect.OpenSettings)
            DrawerContract.Event.OnSyncAccount -> onSyncAccount()
            DrawerContract.Event.OnSyncAllAccounts -> onSyncAllAccounts()
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
            emitEffect(DrawerContract.Effect.OpenAccount(account.id))
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
            emitEffect(DrawerContract.Effect.OpenFolder(folder.folder.id))
        } else if (folder is DisplayUnifiedFolder) {
            emitEffect(DrawerContract.Effect.OpenUnifiedFolder)
        }

        viewModelScope.launch {
            delay(DRAWER_CLOSE_DELAY)
            emitEffect(DrawerContract.Effect.CloseDrawer)
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

/**
 * Delay before closing the drawer to avoid the drawer being closed immediately and give time
 * for the ripple effect to finish.
 */
private const val DRAWER_CLOSE_DELAY = 250L
