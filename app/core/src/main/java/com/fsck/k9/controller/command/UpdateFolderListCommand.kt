package com.fsck.k9.controller.command

import com.fsck.k9.Account
import com.fsck.k9.backend.BackendManager
import com.fsck.k9.preferences.AccountManager
import kotlinx.datetime.Clock
import timber.log.Timber

data class UpdateFolderListArgument(
    val accountUuid: String,
)

class UpdateFolderListCommand(
    private val argument: UpdateFolderListArgument,
    private val accountManager: AccountManager,
    private val backendManager: BackendManager,
    private val clock: Clock,
) : Command {

    override fun invoke() {
        val account = accountManager.getAccount(argument.accountUuid)

        if (account == null) {
            Timber.e("Account not found: ${argument.accountUuid}")
            error("Account not found: ${argument.accountUuid}")
        } else {
            if (isStale(account)) {
                Timber.d("Folder list is stale, updating now for account ${argument.accountUuid}")
                refreshFolderList(account)
            } else {
                Timber.d("Folder list not stale, no update necessary for account ${argument.accountUuid}")
            }
        }
    }

    private fun isStale(account: Account): Boolean {
        val lastRefreshTime = account.lastFolderListRefreshTime
        val now = clock.now()

        return now.toEpochMilliseconds() - lastRefreshTime > FOLDER_LIST_STALENESS_THRESHOLD
    }

    private fun refreshFolderList(account: Account) {
        val backend = backendManager.getBackend(account)
        backend.refreshFolderList()

        val now = clock.now()
        Timber.d("Folder list successfully updated @ %tc", now)

        account.lastFolderListRefreshTime = now.toEpochMilliseconds()

        accountManager.saveAccount(account)
    }

    companion object {
        private const val FOLDER_LIST_STALENESS_THRESHOLD = 24 * 60 * 60 * 1000L
    }
}
