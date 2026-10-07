package net.thunderbird.feature.navigation.drawer.dropdown.domain.entity

import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.UnifiedAccountId

data class UnifiedDisplayAccount(
    override val unreadMessageCount: Int,
    override val starredMessageCount: Int,
    override val hasError: Boolean,
) : DisplayAccount {
    override val id: AccountId = UnifiedAccountId
}
