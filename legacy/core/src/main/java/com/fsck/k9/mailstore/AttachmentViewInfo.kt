package com.fsck.k9.mailstore

import android.net.Uri
import androidx.core.net.toUri
import com.fsck.k9.helper.MimeTypeUtil
import com.fsck.k9.mail.Part
import net.thunderbird.feature.mail.message.reader.api.domain.mapper.AttachmentViewInfoMapper
import net.thunderbird.feature.mail.message.reader.api.ui.attachment.AttachmentId
import net.thunderbird.feature.mail.message.reader.api.ui.attachment.AttachmentUiItem

/**
 * Represents metadata and state information for an email attachment in the view layer.
 *
 * @param mimeType The MIME type of the attachment, may be null if unknown.
 * @param displayName The display name of the attachment file.
 * @param size The size of the attachment in bytes, or UNKNOWN_SIZE if not available.
 * @property internalUri A content provider URI that can be used to retrieve the decoded attachment.
 * Note: All content providers must support an alternative MIME type appended as last URI segment.
 * @param inlineAttachment Whether this attachment is meant to be displayed inline.
 * @param part The underlying Part object containing the attachment data, may be null.
 * @param isContentAvailable Whether the attachment content is currently available for access.
 */
class AttachmentViewInfo(
    @JvmField val mimeType: String?,
    @JvmField val displayName: String?,
    @JvmField val size: Long,
    @JvmField val internalUri: Uri,
    @JvmField val inlineAttachment: Boolean,
    @JvmField val part: Part?,
    override var isContentAvailable: Boolean,
) : AttachmentViewInfoMapper.AttachmentMetadata<Part> {
    fun setContentAvailable() {
        this.isContentAvailable = true
    }

    override val uri: String = internalUri.toString()
    override val filename: String? = displayName

    override val isSupportedImage: Boolean
        get() {
            if (mimeType == null) {
                return false
            }

            return MimeTypeUtil.isSupportedImageType(mimeType) ||
                (
                    MimeTypeUtil.isSameMimeType(
                        MimeTypeUtil.DEFAULT_ATTACHMENT_MIME_TYPE,
                        mimeType,
                    ) &&
                        MimeTypeUtil.isSupportedImageExtension(displayName)
                    )
        }

    override fun getSize(): Long = size
    override fun isInlineAttachment(): Boolean = inlineAttachment

    override fun getMimeType(): String? = mimeType
    override fun getPart(): Part? = part

    companion object {
        const val UNKNOWN_SIZE: Long = -1
    }
}

class DefaultAttachmentViewInfoMapper(
    private val sizeFormatter: (Long) -> String,
) : AttachmentViewInfoMapper<Part> {
    override fun AttachmentViewInfoMapper.AttachmentMetadata<Part>.toUiItem(
        encrypted: Boolean,
    ): AttachmentUiItem<Part> {
        val size = getSize()
        val formattedSize = when (size) {
            AttachmentViewInfo.UNKNOWN_SIZE -> ""
            else -> sizeFormatter(size)
        }
        return when {
            isSupportedImage && isContentAvailable ->
                AttachmentUiItem.RemoteImage(
                    id = AttachmentId(uri),
                    url = uri,
                    filename = filename,
                    formattedSize = formattedSize,
                    size = size,
                    mimeType = getMimeType(),
                    part = getPart(),
                    downloaded = isContentAvailable,
                    encrypted = encrypted,
                )

            isSupportedImage && isInlineAttachment() ->
                AttachmentUiItem.InlinedImage(
                    id = AttachmentId(uri),
                    rawBase64 = uri,
                    filename = filename,
                    formattedSize = formattedSize,
                    size = size,
                    mimeType = getMimeType(),
                    part = getPart(),
                    downloaded = isContentAvailable,
                    encrypted = encrypted,
                )

            isInlineAttachment() -> AttachmentUiItem.InlinedFile(
                id = AttachmentId(uri),
                filename = filename,
                formattedSize = formattedSize,
                size = size,
                mimeType = getMimeType(),
                part = getPart(),
                downloaded = isContentAvailable,
                encrypted = encrypted,
            )

            else -> AttachmentUiItem.File(
                id = AttachmentId(uri),
                filename = filename,
                formattedSize = formattedSize,
                size = size,
                mimeType = getMimeType(),
                part = getPart(),
                downloaded = isContentAvailable,
                encrypted = encrypted,
            )
        }
    }

    override fun AttachmentUiItem<Part>.toDomainItem(): AttachmentViewInfoMapper.AttachmentMetadata<Part> =
        AttachmentViewInfo(
            mimeType = mimeType,
            displayName = filename,
            size = size,
            internalUri = id.uri.toUri(),
            inlineAttachment = this is AttachmentUiItem.InlinedImage || this is AttachmentUiItem.InlinedFile,
            part = part,
            isContentAvailable = downloaded,
        )
}
