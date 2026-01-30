package net.thunderbird.feature.mail.message.list.ui.state

import net.thunderbird.feature.mail.folder.api.FolderType

/**
 * Represents a folder in the mail account hierarchy.
 *
 * This data class holds information about a specific folder, including its identity, name, type,
 * and its relationship within the folder structure (account, parent, root).
 *
 * @property id The unique identifier for the folder.
 * @property name The display name of the folder.
 * @property type The type of the folder (e.g., Inbox, Sent, Drafts). See [FolderType].
 * @property account The [Account] this folder belongs to.
 * @property parent The immediate parent [Folder] in the hierarchy. `null` for top-level folders.
 * @property root The ultimate root [Folder] of the hierarchy. Can be `null` if this is the root
 *  itself or unassigned.
 * @property canExpunge `true` if the current folder can be expunged; `false` otherwise.
 */
data class Folder(
    val id: String,
    val account: Account,
    val name: String,
    val type: FolderType,
    val parent: Folder? = null,
    val root: Folder? = null,
    val canExpunge: Boolean = false,
)
