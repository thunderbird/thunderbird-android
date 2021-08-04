package com.fsck.k9.ui.account

import com.fsck.k9.Account
import com.fsck.k9.MessageCounts

data class DisplayAccount(val account: Account, val messageCounts: MessageCounts)
