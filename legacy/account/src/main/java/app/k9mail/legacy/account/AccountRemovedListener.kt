package app.k9mail.legacy.account

fun interface AccountRemovedListener {
    fun onAccountRemoved(account: Account)
}
