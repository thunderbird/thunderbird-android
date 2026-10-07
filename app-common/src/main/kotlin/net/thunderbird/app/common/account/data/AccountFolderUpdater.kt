package net.thunderbird.app.common.account.data

import com.fsck.k9.mailstore.LocalStoreProvider
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.account.AccountId

internal interface AccountFolderUpdater {
    fun resetVisibleLimits(accountId: AccountId, visibleLimit: Int)
}

internal class LegacyAccountFolderUpdater(
    private val localStoreProvider: LocalStoreProvider,
    private val logger: Logger,
) : AccountFolderUpdater {
    override fun resetVisibleLimits(accountId: AccountId, visibleLimit: Int) {
        try {
            localStoreProvider.getInstance(accountId).resetVisibleLimits(visibleLimit)
        } catch (e: MessagingException) {
            logger.error(throwable = e) { "Failed to reset folder visible limits" }
        }
    }
}
