package net.thunderbird.core.android.account

fun interface AccountRemovedListener {
    fun onAccountRemoved(account: LegacyAccountDto)
}
