package net.thunderbird.feature.account.avatar.ui

import androidx.compose.ui.graphics.vector.ImageVector
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons

/**
 * Maps avatar icon names to design system [Icons].
 *
 * Names must be stable as they are persisted. Unknown names fall back to [Icons.Outlined.Person].
 */
object AvatarIconMapper {

    /**
     * Returns the icon image for the given [name], or the default icon if unknown.
     */
    fun toIcon(name: String): ImageVector {
        return when (name.lowercase()) {
            "star" -> Icons.Outlined.Star
            "person" -> Icons.Outlined.Person
            "folder" -> Icons.Outlined.Folder
            "pets" -> Icons.Outlined.Pets
            "rocket" -> Icons.Outlined.Rocket
            "spa" -> Icons.Outlined.Spa
            else -> Icons.Outlined.Person
        }
    }
}
