package net.thunderbird.feature.navigation.drawer.siderail.domain.entity

/**
 * Represents a unified folder in the drawer.
 *
 * The id is unique for each unified folder type.
 */
internal enum class DisplayUnifiedFolderType(
    val id: String,
) {
    INBOX("unified_inbox"),
}
