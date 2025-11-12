package net.thunderbird.feature.account.avatar

/**
 * A catalog that provides a collection of icons suitable for user avatars.
 *
 * The [defaultName] must always be present in [all].
 *
 * @param TIcon The type representing the icon, e.g., an ImageVector or Drawable.
 */
interface AvatarIconCatalog<TIcon> {

    /**
     * The default icon to use when an unknown name is requested.
     */
    val defaultIcon: TIcon

    /**
     * The stable string name of the default icon.
     */
    val defaultName: String

    /**
     * Returns the [TIcon] associated with the given [name].
     *
     * If the name is not found, it returns the [defaultIcon].
     *
     * @param name The stable, case-insensitive string identifier for the icon.
     * @return The corresponding [TIcon].
     */
    fun toIcon(name: String): TIcon

    /**
     * Returns a list of all available icon names in the catalog.
     */
    fun allNames(): List<String>

    /**
     * Checks if an icon with the given [name] exists in the catalog.
     *
     * @param name The case-insensitive string identifier to check.
     */
    fun contains(name: String): Boolean
}
