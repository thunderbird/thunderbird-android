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
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayTreeFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.MailDisplayFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.UnifiedDisplayFolder
import net.thunderbird.feature.navigation.drawer.dropdown.ui.DrawerContract.Effect
import net.thunderbird.feature.navigation.drawer.dropdown.ui.DrawerContract.Event
import net.thunderbird.feature.navigation.drawer.dropdown.ui.DrawerContract.State
import net.thunderbird.feature.navigation.drawer.dropdown.ui.DrawerContract.ViewModel

/**
 * Delay before closing the drawer to avoid the drawer being closed immediately and give time
 * for the ripple effect to finish.
 */
private const val DRAWER_CLOSE_DELAY = 250L
private const val ACCOUNT_CLOSE_DELAY = 150L

@Suppress("MagicNumber", "TooManyFunctions")
internal class DrawerViewModel(
    private val getDrawerConfig: UseCase.GetDrawerConfig,
    private val saveDrawerConfig: UseCase.SaveDrawerConfig,
    private val getDisplayAccounts: UseCase.GetDisplayAccounts,
    private val getDisplayFoldersForAccount: UseCase.GetDisplayFoldersForAccount,
    private val getDisplayTreeFolder: UseCase.GetDisplayTreeFolder,
    private val syncAccount: UseCase.SyncAccount,
    private val syncAllAccounts: UseCase.SyncAllAccounts,
    private val maxNestingLevel: Int = 2,
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

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun loadAccounts() {
        state.map { it.config.showUnifiedFolders }
            .distinctUntilChanged()
            .flatMapLatest { showUnifiedFolders ->
                getDisplayAccounts(showUnifiedFolders)
            }.collectLatest { accounts ->
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
            it.selectedAccountId
        }.filterNotNull()
            .distinctUntilChanged()
            .flatMapLatest { accountId ->
                getDisplayFoldersForAccount(accountId)
            }.collect { folders ->
                updateFolders(folders, getDisplayTreeFolder(folders, maxNestingLevel))
            }
    }

    private fun updateFolders(displayFolders: List<DisplayFolder>, rootFolder: DisplayTreeFolder) {
        // First try to find the folder in the flat list
        var selectedFolder = displayFolders.find {
            it.id == state.value.selectedFolderId
        }

        // If not found, try to find it in the tree hierarchy
        if (selectedFolder == null) {
            selectedFolder = findFolderById(rootFolder, state.value.selectedFolderId)
        }

        // If still not found, default to the first folder
        if (selectedFolder == null) {
            selectedFolder = displayFolders.firstOrNull() ?: rootFolder.children.firstOrNull()?.displayFolder
        }

        updateState {
            it.copy(
                rootFolder = rootFolder,
                folders = displayFolders.toImmutableList(),
                selectedFolderId = selectedFolder?.id,
                selectedFolder = selectedFolder,
            )
        }
    }

    /**
     * Recursively searches for a folder with the given ID in the DisplayTreeFolder hierarchy.
     */
    private fun findFolderById(treeFolder: DisplayTreeFolder, folderId: String?): DisplayFolder? {
        if (folderId == null) return null

        return if (treeFolder.displayFolder?.id == folderId) {
            treeFolder.displayFolder
        } else {
            // Recursively search in children
            var folder: DisplayFolder? = null
            for (child in treeFolder.children) {
                val found = findFolderById(child, folderId)
                if (found != null) {
                    folder = found
                    break
                }
            }

            folder
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
                viewModelScope.launch {
                    saveDrawerConfig(
                        state.value.config.copy(showAccountSelector = state.value.config.showAccountSelector.not()),
                    ).launchIn(viewModelScope)
                    delay(ACCOUNT_CLOSE_DELAY)
                    updateState {
                        it.copy(showAccountSelection = it.showAccountSelection.not())
                    }
                }
            }

            Event.OnManageFoldersClick -> emitEffect(Effect.OpenManageFolders)
            Event.OnSettingsClick -> emitEffect(Effect.OpenSettings)
            Event.OnSyncAccount -> onSyncAccount()
            Event.OnSyncAllAccounts -> onSyncAllAccounts()
            Event.OnAddAccountClick -> emitEffect(Effect.OpenAddAccount)
        }
    }

    private fun selectAccount(accountId: String?) {
        if (accountId != state.value.selectedAccountId) {
            viewModelScope.launch {
                updateState {
                    it.copy(
                        selectedAccountId = accountId,
                    )
                }
                delay(ACCOUNT_CLOSE_DELAY)
                updateState {
                    it.copy(
                        showAccountSelection = false,
                    )
                }
            }
        }
    }

    private fun selectFolder(folderId: String?) {
        // Find the folder with the given ID
        val folder = folderId?.let {
            state.value.folders.find { it.id == folderId }
                // If not found, try to find it in the tree hierarchy
                ?: findFolderById(state.value.rootFolder, folderId)
        }

        updateState {
            it.copy(
                selectedFolderId = folderId,
                selectedFolder = folder,
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
        // Update the selected folder ID in the state
        selectFolder(folder.id)

        if (folder is MailDisplayFolder) {
            if (folder.accountId != null) {
                emitEffect(
                    Effect.OpenFolder(
                        accountId = folder.accountId,
                        folderId = folder.folder.id,
                    ),
                )
            }
        } else if (folder is UnifiedDisplayFolder) {
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
