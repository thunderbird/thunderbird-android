package com.fsck.k9.ui.messageview

import app.k9mail.legacy.message.controller.MessagingListener
import com.fsck.k9.mail.Part

interface AttachmentLoadingController {
    fun loadAttachment(
        part: Part?,
        listener: MessagingListener
    )
}
