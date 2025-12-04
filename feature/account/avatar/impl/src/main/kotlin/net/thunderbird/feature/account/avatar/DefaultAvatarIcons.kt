package net.thunderbird.feature.account.avatar

import androidx.compose.ui.graphics.vector.ImageVector
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons

/**
 * An enumeration of avatar icons represented as [ImageVector]s.
 *
 * Each icon has a unique identifier derived from its name in lowercase.
 *
 * @property icon The [ImageVector] representing the icon.
 */
enum class DefaultAvatarIcons(
    override val icon: ImageVector,
) : AvatarIcon<ImageVector> {

    Star(icon = Icons.Filled.Star),
    Person(icon = Icons.Outlined.Person),
    Hearth(icon = Icons.Outlined.Hearth),
    Book(icon = Icons.Outlined.Book),
    Flower(icon = Icons.Outlined.Flower),
    Spa(icon = Icons.Outlined.Spa),
    Work(icon = Icons.Outlined.Work),
    School(icon = Icons.Outlined.School),
    Pets(icon = Icons.Outlined.Pets),
    Group(icon = Icons.Outlined.Group),
    Fingerprint(icon = Icons.Outlined.Fingerprint),
    Bank(icon = Icons.Outlined.Bank),
    Smile(icon = Icons.Outlined.Smile),
    Badge(icon = Icons.Outlined.Badge),
    Game(icon = Icons.Outlined.Game),
    Image(icon = Icons.Outlined.Image),
    Rocket(icon = Icons.Outlined.Rocket),
    ;

    override val id: String = this.name.lowercase()
}
