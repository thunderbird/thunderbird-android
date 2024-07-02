package com.fsck.k9.ui.messagelist

import com.fsck.k9.Account.SortType
import com.fsck.k9.Account.SortType.SORT_ARRIVAL
import com.fsck.k9.Account.SortType.SORT_ATTACHMENT
import com.fsck.k9.Account.SortType.SORT_DATE
import com.fsck.k9.Account.SortType.SORT_FLAGGED
import com.fsck.k9.Account.SortType.SORT_SENDER
import com.fsck.k9.Account.SortType.SORT_SUBJECT
import com.fsck.k9.Account.SortType.SORT_UNREAD
import com.fsck.k9.ui.R

class SortTypeToastProvider {
    fun getToast(sortType: SortType, ascending: Boolean): Int {
        return if (ascending) {
            when (sortType) {
                SORT_DATE -> R.string.sort_earliest_first
                SORT_ARRIVAL -> R.string.sort_earliest_first
                SORT_SUBJECT -> R.string.sort_subject_alpha
                SORT_SENDER -> R.string.sort_sender_alpha
                SORT_UNREAD -> R.string.sort_unread_first
                SORT_FLAGGED -> R.string.sort_flagged_first
                SORT_ATTACHMENT -> R.string.sort_attach_first
            }
        } else {
            when (sortType) {
                SORT_DATE -> R.string.sort_latest_first
                SORT_ARRIVAL -> R.string.sort_latest_first
                SORT_SUBJECT -> R.string.sort_subject_re_alpha
                SORT_SENDER -> R.string.sort_sender_re_alpha
                SORT_UNREAD -> R.string.sort_unread_last
                SORT_FLAGGED -> R.string.sort_flagged_last
                SORT_ATTACHMENT -> R.string.sort_unattached_first
            }
        }
    }
}
