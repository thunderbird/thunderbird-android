package net.thunderbird.feature.navigation.drawer.dropdown.domain.entity

internal data class DisplayAccount(
    val id: String,
    val name: String,
    val email: String,
    val color: Int,
    val unreadMessageCount: Int,
    val starredMessageCount: Int,
)
