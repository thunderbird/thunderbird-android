package net.thunderbird.feature.account.avatar

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Default avatar icon catalog using [DefaultAvatarIcons].
 */
class DefaultAvatarIconCatalog : AvatarIconCatalog<AvatarIcon<ImageVector>> {

    override val defaultIcon: AvatarIcon<ImageVector> = DefaultAvatarIcons.Person

    override fun get(id: String): AvatarIcon<ImageVector> {
        return DefaultAvatarIcons.entries.firstOrNull { it.id == id.lowercase() } ?: defaultIcon
    }

    override fun all(): List<AvatarIcon<ImageVector>> = DefaultAvatarIcons.entries

    override fun contains(id: String): Boolean = DefaultAvatarIcons.entries.any { it.id == id.lowercase() }
}
