package com.fsck.k9.backend.eas

import com.fsck.k9.mail.Message

class MessageSendCommand(private val client: EasClient,
                         private val provisionManager: EasProvisionManager) {
    fun sendMessage(message: Message) {
        provisionManager.ensureProvisioned {
            client.sendMessage(message)
        }
    }
}
