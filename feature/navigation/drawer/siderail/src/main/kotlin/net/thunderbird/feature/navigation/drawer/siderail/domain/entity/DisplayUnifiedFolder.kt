package net.thunderbird.feature.navigation.drawer.siderail.domain.entity

internal data class DisplayUnifiedFolder(
    override val id: String,
    val unifiedType: DisplayUnifiedFolderType,
    override val unreadMessageCount: Int,
    override val starredMessageCount: Int,
) : DisplayFolder
