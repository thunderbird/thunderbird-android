package app.k9mail.backend.demo

import com.fsck.k9.mail.FolderType
import kotlinx.serialization.Serializable

@Serializable
internal data class DemoFolderData(
    val name: String,
    val type: FolderType,
    val messageServerIds: List<String>,
)
