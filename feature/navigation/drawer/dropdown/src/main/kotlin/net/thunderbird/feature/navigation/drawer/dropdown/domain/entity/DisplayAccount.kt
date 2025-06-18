package net.thunderbird.feature.navigation.drawer.dropdown.domain.entity

interface DisplayAccount {
    val id: String
    val unreadMessageCount: Int
    val starredMessageCount: Int
}
