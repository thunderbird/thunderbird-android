package com.fsck.k9.ui.messagelist

import androidx.annotation.StringRes
import com.fsck.k9.ui.R

/**
 * What the message list shows instead of messages when there are none.
 */
internal enum class MessageListEmptyState(
    @get:StringRes val messageResId: Int,
    @get:StringRes val hintResId: Int? = null,
) {
    NO_MESSAGES(R.string.message_list_empty),
    NO_RESULTS(R.string.message_list_empty_search),
    NOT_SYNCED(R.string.message_list_not_synced),
    NOT_SYNCED_PULL_TO_SYNC(R.string.message_list_not_synced, R.string.message_list_not_synced_hint),
    ;

    companion object {
        fun of(isSearch: Boolean, hasUnsyncedFolders: Boolean, isPullToSyncAllowed: Boolean): MessageListEmptyState {
            return when {
                isSearch -> NO_RESULTS
                hasUnsyncedFolders && isPullToSyncAllowed -> NOT_SYNCED_PULL_TO_SYNC
                hasUnsyncedFolders -> NOT_SYNCED
                else -> NO_MESSAGES
            }
        }
    }
}
