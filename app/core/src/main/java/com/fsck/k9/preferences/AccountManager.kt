package com.fsck.k9.preferences

import com.fsck.k9.AccountRemovedListener

interface AccountManager {
    fun addAccountRemovedListener(listener: AccountRemovedListener)
}
