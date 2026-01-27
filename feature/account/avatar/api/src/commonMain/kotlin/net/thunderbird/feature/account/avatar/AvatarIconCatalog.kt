package net.thunderbird.feature.account.avatar

/**
 * A catalog that provides a collection of icons suitable for user avatars.
 *
 * The [defaultIcon] must always be present in [all].
 *
 * @param TIcon The AvatarIcon type used in the catalog.
 */
interface AvatarIconCatalog<TIcon : AvatarIcon<*>> {

    /**
     * The default icon to use when an unknown icon id is requested.
     */
    val defaultIcon: TIcon

    /**
     * Returns the [TIcon] associated with the given [id].
     *
     * If the name is not found, it returns the [defaultIcon].
     *
     * @param id The stable, case-insensitive string identifier for the icon.
     * @return The corresponding [TIcon].
     */
    fun get(id: String): TIcon

    /**
     * Returns a list of all available icons in the catalog.
     */
    fun all(): List<TIcon>

    /**
     * Checks if an icon exists in the catalog.
     *
     * @param id The stable, case-insensitive string identifier for the icon.
     */
    fun contains(id: String): Boolean
}
