package net.thunderbird.feature.account.avatar

import net.thunderbird.feature.account.avatar.AvatarIconCatalog

/**
 * Default catalog of avatar icon names used across the app.
 * These names are stable identifiers; UI maps them to actual vectors via AvatarIconMapper.
 */
class DefaultAvatarIconCatalog : AvatarIconCatalog {
    private val names = listOf(
        DEFAULT,
        "folder",
        "pets",
        "rocket",
        "star",
        "spa",
    )

    override val defaultName: String = DEFAULT

    override fun all(): List<String> = names

    internal companion object {
        const val DEFAULT = "person"
    }
}
