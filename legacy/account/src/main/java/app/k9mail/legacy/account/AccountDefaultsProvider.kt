package app.k9mail.legacy.account

fun interface AccountDefaultsProvider {
    fun applyDefaults(account: Account)
}
