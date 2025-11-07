package net.thunderbird.feature.navigation.drawer.dropdown.domain.entity

import net.thunderbird.feature.account.avatar.Avatar

internal data class MailDisplayAccount(
    override val id: String,
    val name: String,
    val email: String,
    val color: Int,
    val avatar: Avatar = Avatar.Monogram("?"),
    override val unreadMessageCount: Int,
    override val starredMessageCount: Int,
    override val hasError: Boolean,
) : DisplayAccount
