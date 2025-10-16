package net.thunderbird.core.android.account

import net.thunderbird.feature.account.AccountId

fun interface AccountRemovedListener {
    fun onAccountRemoved(id: AccountId)
}
