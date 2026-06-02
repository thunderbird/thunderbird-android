package net.thunderbird.feature.mail.message.reader.api.ui.attachment

import androidx.compose.runtime.Immutable

@Immutable
sealed interface AttachmentUiItem<out TPart> {
    val id: AttachmentId
    val filename: String?
    val formattedSize: String
    val size: Long
    val mimeType: String?
    val part: TPart?
    val downloaded: Boolean
    val encrypted: Boolean

    data class RemoteImage<out TPart>(
        override val id: AttachmentId,
        val url: String,
        override val filename: String?,
        override val formattedSize: String,
        override val size: Long,
        override val mimeType: String? = null,
        override val part: TPart? = null,
        override val downloaded: Boolean = false,
        override val encrypted: Boolean = false,
    ) : AttachmentUiItem<TPart>

    data class InlinedImage<out TPart>(
        override val id: AttachmentId,
        val rawBase64: String,
        override val filename: String?,
        override val formattedSize: String,
        override val size: Long,
        override val mimeType: String? = null,
        override val part: TPart? = null,
        override val downloaded: Boolean = false,
        override val encrypted: Boolean = false,
    ) : AttachmentUiItem<TPart>

    data class File<out TPart>(
        override val id: AttachmentId,
        override val filename: String?,
        override val formattedSize: String,
        override val size: Long,
        override val mimeType: String? = null,
        override val part: TPart? = null,
        override val downloaded: Boolean = false,
        override val encrypted: Boolean = false,
    ) : AttachmentUiItem<TPart>

    data class InlinedFile<out TPart>(
        override val id: AttachmentId,
        override val filename: String?,
        override val formattedSize: String,
        override val size: Long,
        override val mimeType: String? = null,
        override val part: TPart? = null,
        override val downloaded: Boolean = false,
        override val encrypted: Boolean = false,
    ) : AttachmentUiItem<TPart>
}

@JvmInline
value class AttachmentId(val uri: String)
