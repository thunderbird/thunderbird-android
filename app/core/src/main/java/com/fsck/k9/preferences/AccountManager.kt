package com.fsck.k9.preferences

import com.fsck.k9.Account
import com.fsck.k9.AccountRemovedListener

interface AccountManager {
    fun getAccount(accountUuid: String): Account?
    fun addAccountRemovedListener(listener: AccountRemovedListener)
    fun moveAccount(account: Account, newPosition: Int)
}
