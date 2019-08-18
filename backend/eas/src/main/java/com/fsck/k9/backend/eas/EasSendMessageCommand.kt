package com.fsck.k9.backend.eas

import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.filter.EOLConvertingOutputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream


class EasSendMessageCommand(private val client: EasClient,
                            private val provisionManager: EasProvisionManager) {

    fun sendMessage(message: Message) {
        val out = ByteArrayOutputStream(message.size.toInt())

        val msgOut = EOLConvertingOutputStream(BufferedOutputStream(out, 1024))
        message.writeTo(msgOut)
        msgOut.flush()

        val data = out.toByteArray()

        provisionManager.ensureProvisioned {
            client.sendMessage(data)
        }
    }
}
