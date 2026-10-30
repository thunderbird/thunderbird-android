package com.fsck.k9.ui.messageview

import app.k9mail.legacy.message.controller.MessagingListener
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.mail.Part
import com.fsck.k9.mailstore.LocalPart

class DefaultAttachmentLoadingController(
    private val messagingController: MessagingController,
) : AttachmentLoadingController {
    override fun loadAttachment(part: Part?, listener: MessagingListener) {
        val localPart = part as LocalPart
        val accountId = localPart.accountId
        val message = localPart.message
        messagingController.loadAttachment(accountId, message, part, listener)
    }
}
