package net.thunderbird.feature.navigation.drawer.dropdown.domain.entity

internal data class MailDisplayAccount(
    override val id: String,
    val name: String,
    val email: String,
    val color: Int,
    override val unreadMessageCount: Int,
    override val starredMessageCount: Int,
) : DisplayAccount
