package com.fsck.k9.ui.settings.account

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.fsck.k9.mailstore.SpecialFolderSelectionStrategy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.thunderbird.components.core.outcome.fold
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.FolderType
import net.thunderbird.feature.mail.folder.api.RemoteFolder
import net.thunderbird.feature.mail.folder.api.data.repository.RemoteFolderQueryRepository

class AccountSettingsViewModel(
    private val accountManager: LegacyAccountManager,
    private val remoteFolderQueryRepository: RemoteFolderQueryRepository,
    private val specialFolderSelectionStrategy: SpecialFolderSelectionStrategy,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    val accounts = accountManager.observeAll().asLiveData()
    private var accountId: AccountId? = null
    private val accountLiveData = MutableLiveData<LegacyAccount?>()
    private val foldersLiveData = MutableLiveData<RemoteFolderInfo>()

    fun getAccount(accountId: AccountId): LiveData<LegacyAccount?> {
        if (this.accountId != accountId) {
            this.accountId = accountId
            viewModelScope.launch {
                val account = withContext(backgroundDispatcher) {
                    loadAccount(accountId)
                }
                accountLiveData.value = account
            }
        }

        return accountLiveData
    }

    /**
     * Returns the cached [LegacyAccount] if possible. Otherwise does a blocking load because
     * `PreferenceFragmentCompat` doesn't support asynchronous preference loading.
     */
    fun getAccountBlocking(accountId: AccountId): LegacyAccount {
        return accountLiveData.value
            ?: loadAccount(accountId).also { account ->
                this.accountId = accountId
                accountLiveData.value = account
            }
            ?: error("Account $accountId not found")
    }

    private fun loadAccount(accountId: AccountId): LegacyAccount? {
        return accountManager.findById(accountId)
    }

    fun getFolders(accountId: AccountId): LiveData<RemoteFolderInfo> {
        if (foldersLiveData.value == null) {
            loadFolders(accountId)
        }

        return foldersLiveData
    }

    private fun loadFolders(accountId: AccountId) {
        viewModelScope.launch {
            val remoteFolderInfo = withContext(backgroundDispatcher) {
                val folders = remoteFolderQueryRepository.getAllByAccountId(accountId)
                    .fold(
                        onSuccess = { it },
                        onFailure = { error ->
                            when (val throwable = error.throwable) {
                                null -> error("Unknown error while loading folders. Error: $error")
                                else -> throw throwable
                            }
                        },
                    )
                    .sortedWith(
                        compareByDescending<RemoteFolder> { it.type == FolderType.INBOX }
                            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name },
                    )

                val automaticSpecialFolders = getAutomaticSpecialFolders(folders)
                RemoteFolderInfo(folders, automaticSpecialFolders)
            }
            foldersLiveData.value = remoteFolderInfo
        }
    }

    private fun getAutomaticSpecialFolders(folders: List<RemoteFolder>): Map<FolderType, RemoteFolder?> {
        return mapOf(
            FolderType.ARCHIVE to specialFolderSelectionStrategy.selectSpecialFolder(folders, FolderType.ARCHIVE),
            FolderType.DRAFTS to specialFolderSelectionStrategy.selectSpecialFolder(folders, FolderType.DRAFTS),
            FolderType.SENT to specialFolderSelectionStrategy.selectSpecialFolder(folders, FolderType.SENT),
            FolderType.SPAM to specialFolderSelectionStrategy.selectSpecialFolder(folders, FolderType.SPAM),
            FolderType.TRASH to specialFolderSelectionStrategy.selectSpecialFolder(folders, FolderType.TRASH),
        )
    }
}

data class RemoteFolderInfo(
    val folders: List<RemoteFolder>,
    val automaticSpecialFolders: Map<FolderType, RemoteFolder?>,
)
