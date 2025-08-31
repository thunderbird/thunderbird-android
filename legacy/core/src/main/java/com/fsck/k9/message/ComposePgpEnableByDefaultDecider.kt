package com.fsck.k9.message

import com.fsck.k9.crypto.MessageCryptoStructureDetector
import com.fsck.k9.mail.Message

class ComposePgpEnableByDefaultDecider {
    fun shouldEncryptByDefault(localMessage: Message?) = messageIsEncrypted(localMessage)

    private fun messageIsEncrypted(localMessage: Message?) =
        localMessage?.let {
            MessageCryptoStructureDetector.findMultipartEncryptedParts(it).isNotEmpty()
        } ?: false
}
