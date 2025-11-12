package net.thunderbird.feature.account.avatar

import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableMap
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons

/**
 * Default catalog of avatar icon names used across the app.
 * These names are stable identifiers; UI maps them to actual vectors via AvatarIconMapper.
 */
class ImageVectorAvatarIconCatalog : AvatarIconCatalog<ImageVector> {

    override val defaultIcon: ImageVector = Icons.Outlined.Person
    override val defaultName: String = "person"

    private val iconMap: ImmutableMap<String, ImageVector> = mapOf(
        "star" to Icons.Filled.Star,
        "person" to Icons.Outlined.Person,
        "hearth" to Icons.Outlined.Hearth,
        "book" to Icons.Outlined.Book,
        "flower" to Icons.Outlined.Flower,
        "spa" to Icons.Outlined.Spa,
        "work" to Icons.Outlined.Work,
        "school" to Icons.Outlined.School,
        "pets" to Icons.Outlined.Pets,
        "group" to Icons.Outlined.Group,
        "fingerprint" to Icons.Outlined.Fingerprint,
        "bank" to Icons.Outlined.Bank,
        "smile" to Icons.Outlined.Smile,
        "badge" to Icons.Outlined.Badge,
        "game" to Icons.Outlined.Game,
        "image" to Icons.Outlined.Image,
        "rocket" to Icons.Outlined.Rocket,
    ).toImmutableMap()

    override fun toIcon(name: String): ImageVector {
        return iconMap[name.lowercase()] ?: defaultIcon
    }

    override fun allNames(): List<String> {
        return iconMap.keys.toList()
    }

    override fun contains(name: String): Boolean {
        return iconMap.containsKey(name.lowercase())
    }
}
