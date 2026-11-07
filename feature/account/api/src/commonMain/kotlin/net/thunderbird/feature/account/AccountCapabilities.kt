package net.thunderbird.feature.account

interface AccountCapabilities {
    fun supportsUpload(accountId: AccountId): Boolean
}
