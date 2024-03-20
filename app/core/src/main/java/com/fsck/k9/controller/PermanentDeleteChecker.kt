package com.fsck.k9.controller

import androidx.annotation.WorkerThread
import com.fsck.k9.Account
import com.fsck.k9.Account.DeletePolicy
import com.fsck.k9.preferences.AccountManager

/**
 * Checks whether deleting a list of messages in the app will permanently delete none, some, or all of them.
 *
 * Deleting a message permanently means both the local copy and the server copy will be deleted.
 */
class PermanentDeleteChecker internal constructor(
    private val accountManager: AccountManager,
    private val localFolderChecker: LocalFolderChecker,
    private val deleteOperationDecider: DeleteOperationDecider,
) {
    /**
     * Check whether deleting messages will delete some of them permanently.
     *
     * Note: This method can perform disk I/O. Don't call it from the main thread!
     */
    @WorkerThread
    fun checkPermanentDelete(messageReferences: List<MessageReference>): PermanentDeleteResult {
        val permanentDeleteCount = messageReferences
            .groupBy { it.accountUuid }
            .mapKeysNotNull { accountUuid ->
                accountManager.getAccount(accountUuid)
            }
            .map { (account, messageReferences) ->
                getPermanentDeleteCount(account, messageReferences)
            }
            .sum()

        return when (permanentDeleteCount) {
            0 -> PermanentDeleteResult.None
            messageReferences.size -> PermanentDeleteResult.All
            else -> PermanentDeleteResult.Some(permanentDeleteCount)
        }
    }

    private fun getPermanentDeleteCount(account: Account, messageReferences: List<MessageReference>): Int {
        return messageReferences
            .groupBy { it.folderId }
            .map { (folderId, messageReferences) ->
                if (isPermanentDelete(account, folderId)) {
                    messageReferences.size
                } else {
                    0
                }
            }
            .sum()
    }

    private fun isPermanentDelete(account: Account, folderId: Long): Boolean {
        return deleteOperationDecider.isDeleteImmediately(account, folderId) && !isRemoteCopyKept(account, folderId)
    }

    private fun isRemoteCopyKept(account: Account, folderId: Long): Boolean {
        val isRemoteFolder = !localFolderChecker.isLocalFolder(account, folderId)

        // Only DeletePolicy.ON_DELETE will actually remove messages from the server.
        val keepRemoteCopy = account.deletePolicy != DeletePolicy.ON_DELETE

        return isRemoteFolder && keepRemoteCopy
    }
}

private fun <K, V, R : Any> Map<K, V>.mapKeysNotNull(transform: (K) -> R?): Map<R, V> {
    return buildMap {
        this@mapKeysNotNull.forEach { (key, value) ->
            transform(key)?.let { newKey ->
                put(newKey, value)
            }
        }
    }
}

sealed interface PermanentDeleteResult {
    data object None : PermanentDeleteResult
    data class Some(val permanentDeleteCount: Int) : PermanentDeleteResult
    data object All : PermanentDeleteResult
}
