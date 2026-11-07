package com.fsck.k9.ui.messageview

import com.fsck.k9.helper.MimeTypeUtil

/**
 * Returns the MIME type to pass to the "create document" dialog when saving an attachment.
 *
 * Document providers (e.g. the Downloads provider) only treat the part after the last dot as a file extension when
 * it matches the MIME type of the document. When an attachment uses the generic `application/octet-stream` type,
 * a duplicate of `file.pdf` would therefore be saved as `file.pdf (1)` instead of `file (1).pdf`.
 *
 * To avoid this, the MIME type is inferred from the file name when the attachment uses the generic type. This only
 * affects the hint given to the document provider; the file name and the content of the attachment are unchanged.
 */
internal fun getSaveAttachmentMimeType(displayName: String?, mimeType: String): String {
    return if (MimeTypeUtil.isDefaultMimeType(mimeType)) {
        MimeTypeUtil.getMimeTypeByExtension(displayName)
    } else {
        mimeType
    }
}
