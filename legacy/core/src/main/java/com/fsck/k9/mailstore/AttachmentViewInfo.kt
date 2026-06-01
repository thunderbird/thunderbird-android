package com.fsck.k9.mailstore

import android.net.Uri
import com.fsck.k9.helper.MimeTypeUtil
import com.fsck.k9.mail.Part

/**
 * @property internalUri A content provider URI that can be used to retrieve the decoded attachment.
 * Note: All content providers must support an alternative MIME type appended as last URI segment.
 */
class AttachmentViewInfo(
    @JvmField val mimeType: String?,
    @JvmField val displayName: String,
    @JvmField val size: Long,
    @JvmField val internalUri: Uri,
    @JvmField val inlineAttachment: Boolean,
    @JvmField val part: Part?,
    var isContentAvailable: Boolean,
) {
    fun setContentAvailable() {
        this.isContentAvailable = true
    }

    val isSupportedImage: Boolean
        get() {
            if (mimeType == null) {
                return false
            }

            return MimeTypeUtil.isSupportedImageType(mimeType) || (MimeTypeUtil.isSameMimeType(
                MimeTypeUtil.DEFAULT_ATTACHMENT_MIME_TYPE,
                mimeType,
            ) &&
                MimeTypeUtil.isSupportedImageExtension(displayName))
        }

    companion object {
        const val UNKNOWN_SIZE: Long = -1
    }
}
