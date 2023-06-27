package app.k9mail.feature.account.setup.domain.entity

data class AccountOptions(
    val accountName: String,
    val displayName: String,
    val emailSignature: String?,
    val checkFrequencyInMinutes: Int,
    val messageDisplayCount: Int,
    val showNotification: Boolean,
)
