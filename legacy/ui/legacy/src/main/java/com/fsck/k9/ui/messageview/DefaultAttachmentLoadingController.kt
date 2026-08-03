package com.fsck.k9.ui.messageview

import app.k9mail.legacy.message.controller.MessagingListener
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.mail.Part
import com.fsck.k9.mailstore.LocalPart
import net.thunderbird.core.android.account.LegacyAccountDtoManager

class DefaultAttachmentLoadingController(
    private val messagingController: MessagingController,
    private val accountManager: LegacyAccountDtoManager,
    ): AttachmentLoadingController {
    override fun loadAttachment(part: Part?, listener: MessagingListener) {
        val localPart = part as LocalPart
        val message = localPart.message
        val account = accountManager.getAccount(localPart.accountUuid)
        messagingController.loadAttachment(account, message, part, listener)
    }
}
