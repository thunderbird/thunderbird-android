package net.thunderbird.feature.account.avatar

/**
 * Catalog of available avatar icons identified by a stable name String.
 *
 * The [defaultName] must always be present in [all].
 */
interface AvatarIconCatalog {
    /** Stable identifier of the default icon. */
    val defaultName: String

    /** List of all available icon names. Names must be stable for persistence. */
    fun all(): List<String>

    /** Returns true if the provided [name] exists in this catalog. */
    fun contains(name: String): Boolean = all().contains(name)
}
