package app.k9mail.backend.demo

import com.fsck.k9.mail.FolderType
import kotlinx.serialization.Serializable

@Serializable
internal data class DemoFolder(
    val name: String,
    val type: FolderType,
    val messageServerIds: List<String>,
    val subFolders: DemoFolders? = null,
)
