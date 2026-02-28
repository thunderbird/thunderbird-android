package net.thunderbird.feature.navigation.drawer.siderail.domain.entity

import net.thunderbird.feature.account.avatar.Avatar

internal sealed interface DisplayAccount {
    val id: String
    val unreadMessageCount: Int
    val starredMessageCount: Int
}

internal data class MailDisplayAccount(
    override val id: String,
    val name: String,
    val email: String,
    val color: Int,
    val avatar: Avatar,
    override val unreadMessageCount: Int,
    override val starredMessageCount: Int,
) : DisplayAccount

internal data class UnifiedDisplayAccount(
    override val unreadMessageCount: Int,
    override val starredMessageCount: Int,
) : DisplayAccount {
    override val id: String = UNIFIED_ACCOUNT_ID

    companion object {
        const val UNIFIED_ACCOUNT_ID = "unified_account"
    }
}
