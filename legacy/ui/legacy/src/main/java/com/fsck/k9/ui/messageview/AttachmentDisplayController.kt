package com.fsck.k9.ui.messageview

import com.fsck.k9.mailstore.AttachmentViewInfo

interface AttachmentDisplayController {
    fun showAttachmentLoadingDialog()
    fun hideAttachmentLoadingDialogOnMainThread()
    fun refreshAttachmentThumbnail(attachment: AttachmentViewInfo)
}
