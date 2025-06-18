package net.thunderbird.feature.navigation.drawer.dropdown.domain.entity

internal data class UnifiedDisplayAccount(
    override val unreadMessageCount: Int,
    override val starredMessageCount: Int,
) : DisplayAccount {
    override val id: String = UNIFIED_ACCOUNT_ID

    companion object {
        const val UNIFIED_ACCOUNT_ID = "unified_account"
    }
}
