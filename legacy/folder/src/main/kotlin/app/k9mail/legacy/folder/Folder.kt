package app.k9mail.legacy.folder

data class Folder(
    val id: Long,
    val name: String,
    val type: FolderType,
    val isLocalOnly: Boolean,
)
