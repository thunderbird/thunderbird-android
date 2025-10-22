package net.thunderbird.feature.navigation.drawer.dropdown.domain.entity

data class UnifiedDisplayAccount(
    override val unreadMessageCount: Int,
    override val starredMessageCount: Int,
    override val hasError: Boolean,
) : DisplayAccount {
    override val id: String = UNIFIED_ACCOUNT_ID

    companion object {
        const val UNIFIED_ACCOUNT_ID = "unified_account"
    }
}
