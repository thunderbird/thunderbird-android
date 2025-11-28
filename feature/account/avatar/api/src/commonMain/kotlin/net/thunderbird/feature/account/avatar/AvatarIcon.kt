package net.thunderbird.feature.account.avatar

/**
 * Represents an icon that can be used for user avatars.
 *
 * @param TIcon The type representing the icon, e.g., an ImageVector or Drawable.
 */
interface AvatarIcon<TIcon> {
    val id: String
    val icon: TIcon
}
