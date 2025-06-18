package net.thunderbird.feature.navigation.drawer.dropdown.domain.entity

internal data class UnifiedDisplayAccount(
    override val id: String,
    override val unreadMessageCount: Int,
    override val starredMessageCount: Int,
) : DisplayAccount
