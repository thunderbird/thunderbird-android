package app.k9mail.feature.account.common.domain.entity

data class AccountSyncOptions(
    val checkFrequencyInMinutes: Int,
    val messageDisplayCount: Int,
    val showNotification: Boolean,
)
