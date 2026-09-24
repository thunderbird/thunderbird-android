package net.thunderbird.app.common.feature.mail.message.mapper

import android.content.Context
import android.util.Base64
import com.eygraber.uri.toAndroidUri
import com.fsck.k9.mail.Part
import com.fsck.k9.mail.internet.Headers
import com.fsck.k9.mail.internet.MimeBodyPart
import com.fsck.k9.mail.internet.MimeHeader
import com.fsck.k9.mail.internet.MimeUtility
import com.fsck.k9.mailstore.BinaryMemoryBody
import com.fsck.k9.mailstore.LocalPart
import java.io.File
import net.thunderbird.app.common.feature.mail.message.mapper.DefaultMessageDataMapper.Companion.LOG_ID
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.mail.message.MessageAttachment
import net.thunderbird.feature.mail.message.MimeType
import net.thunderbird.feature.mail.message.orDefault

/**
 * Resolves attachment metadata and content when converting between legacy [Part]s and
 * [MessageAttachment]s.
 *
 * Attachment content is copied into the app cache directory, so the resulting URIs remain valid
 * independently of the body backing the original part.
 */
internal class AttachmentResolver(private val logger: Logger, private val context: Context) {
    /**
     * Returns the attachment file name taken from the `Content-Disposition` `filename` parameter,
     * falling back to the `Content-Type` `name` parameter, or `null` when neither is present.
     */
    fun Part.resolveAttachmentName(): String? =
        MimeUtility.getHeaderParameter(disposition, "filename")
            ?: MimeUtility.getHeaderParameter(contentType, "name")

    /**
     * Copies the part's content into a temporary file in the app cache directory.
     *
     * @return The created file, or `null` when the part does not have a body because its content hasn't been
     * downloaded yet.
     */
    fun Part.resolveAttachmentFile(): File? {
        val copiedFile = body?.let { partBody ->
            File.createTempFile("attachment", null, context.cacheDir).apply {
                val input = partBody.inputStream
                try {
                    outputStream().use { output -> input.copyTo(output) }
                } finally {
                    MimeUtility.closeInputStreamWithoutDeletingTemporaryFiles(input)
                }
            }
        }
        if (copiedFile == null) {
            logger.warn {
                "$LOG_ID attachment part has no body (content not downloaded yet). " +
                    "internalUri will be null. part = $this"
            }
        }
        return copiedFile
    }

    /**
     * Determines the attachment size in bytes, preferring the length of [copiedFile], then the size
     * persisted for a [LocalPart], then the `Content-Disposition` `size` parameter, defaulting to `0`.
     *
     * @param copiedFile The file holding the attachment content, or `null` when unavailable.
     */
    fun Part.resolveAttachmentSize(copiedFile: File?): Long =
        copiedFile?.length()
            ?: (this as? LocalPart)?.size
            ?: MimeUtility.getHeaderParameter(disposition, "size")?.toLong()
            ?: 0L

    /**
     * Returns `true` when the part is an inline image, meaning it has an `inline` disposition, a
     * content ID and an `image/` MIME type.
     */
    fun Part.isInline(): Boolean = disposition?.let { MimeUtility.getHeaderParameter(it, null) }
        ?.equals("inline", ignoreCase = true) == true &&
        contentId != null &&
        mimeType?.startsWith(prefix = "image/", ignoreCase = true) == true

    /**
     * Converts this attachment into a base64 encoded [MimeBodyPart].
     *
     * When [MessageAttachment.internalUri] is null, the attachment's content hasn't been downloaded
     * yet (e.g. after a partial sync). There are no bytes to embed, so the resulting body part is
     * created with a null [com.fsck.k9.mail.Body]. This mirrors how [LocalFolder][com.fsck.k9.mailstore.LocalFolder]
     * persists such a part with `data_location = MISSING`: display name and size still round-trip
     * through the Content-Disposition header, and `AttachmentInfoExtractor.extractAttachmentInfoForDatabase`
     * reads them back from there, so the part is stored correctly without requiring content.
     *
     * @param context The context used to read the attachment content from [internalUri].
     */
    fun MessageAttachment.toBodyPart(context: Context): MimeBodyPart {
        val resolvedMimeType = mimeType?.value ?: MimeType.AttachmentDefault.value
        val resolvedName = displayName.orDefault().value

        val body = internalUri?.let { uri ->
            val bytes = context.contentResolver.openInputStream(uri.toAndroidUri())
                ?.use { it.readBytes() }
                ?: byteArrayOf()
            BinaryMemoryBody(Base64.encode(bytes, Base64.DEFAULT), "base64")
        }

        val bodyPart = MimeBodyPart.create(body, Headers.contentType(resolvedMimeType, resolvedName))
        bodyPart.addHeader(
            MimeHeader.HEADER_CONTENT_DISPOSITION,
            Headers.contentDisposition(toDisposition(), resolvedName, size.takeIf { it >= 0 }),
        )
        if (this is MessageAttachment.Inline) {
            contentId?.let { bodyPart.addHeader(MimeHeader.HEADER_CONTENT_ID, it.value) }
        }
        return bodyPart
    }
}
