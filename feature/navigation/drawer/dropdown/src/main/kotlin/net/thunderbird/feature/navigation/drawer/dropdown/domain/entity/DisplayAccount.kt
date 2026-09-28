package net.thunderbird.feature.navigation.drawer.dropdown.domain.entity

import net.thunderbird.feature.account.AccountId

sealed interface DisplayAccount {
    val id: String
    val unreadMessageCount: Int
    val starredMessageCount: Int
    val hasError: Boolean
}
