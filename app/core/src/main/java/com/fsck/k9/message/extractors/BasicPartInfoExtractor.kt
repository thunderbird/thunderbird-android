package com.fsck.k9.message.extractors

import com.fsck.k9.mail.Part
import com.fsck.k9.mail.internet.MimeParameterDecoder
import com.fsck.k9.mail.internet.MimeUtility
import com.fsck.k9.mail.internet.MimeValue
import java.util.Locale

private const val FALLBACK_NAME = "noname"

/**
 * Extract a display name and the size from the headers of a message part.
 */
class BasicPartInfoExtractor {
    fun extractPartInfo(part: Part): BasicPartInfo {
        val contentDisposition = part.disposition?.toMimeValue()

        return BasicPartInfo(
            displayName = part.getDisplayName(contentDisposition),
            size = contentDisposition?.getParameter("size")?.toLongOrNull()
        )
    }

    fun extractDisplayName(part: Part): String {
        return part.getDisplayName()
    }

    private fun Part.getDisplayName(contentDisposition: MimeValue? = disposition?.toMimeValue()): String {
        return contentDisposition?.getParameter("filename")
            ?: contentType?.getParameter("name")
            ?: mimeType.toDisplayName()
    }

    private fun String?.toDisplayName(): String {
        val extension = this?.let { mimeType -> MimeUtility.getExtensionByMimeType(mimeType) }
        return if (extension.isNullOrEmpty()) FALLBACK_NAME else "$FALLBACK_NAME.$extension"
    }

    private fun String.toMimeValue(): MimeValue = MimeParameterDecoder.decode(this)

    private fun MimeValue.getParameter(name: String): String? = parameters[name.toLowerCase(Locale.ROOT)]

    private fun String.getParameter(name: String): String? {
        val mimeValue = MimeParameterDecoder.decode(this)
        return mimeValue.parameters[name.toLowerCase(Locale.ROOT)]
    }
}

data class BasicPartInfo(
    val displayName: String,
    val size: Long?
)
