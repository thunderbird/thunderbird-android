package net.thunderbird.feature.navigation.drawer.dropdown.domain.entity

import kotlinx.collections.immutable.ImmutableList

internal data class DisplayTreeFolder(
    val displayFolder: DisplayFolder?,
    val displayName: String?,
    val totalUnreadCount: Int,
    val totalStarredCount: Int,
    val children: ImmutableList<DisplayTreeFolder>,
)
