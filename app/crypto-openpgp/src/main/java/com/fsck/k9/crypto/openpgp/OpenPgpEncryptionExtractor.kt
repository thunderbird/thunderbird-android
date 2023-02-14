package com.fsck.k9.crypto.openpgp

import com.fsck.k9.crypto.EncryptionExtractor
import com.fsck.k9.crypto.EncryptionResult
import com.fsck.k9.mail.Message
import com.fsck.k9.message.extractors.TextPartFinder

class OpenPgpEncryptionExtractor internal constructor(
    private val encryptionDetector: EncryptionDetector,
) : EncryptionExtractor {

    override fun extractEncryption(message: Message): EncryptionResult? {
        return if (encryptionDetector.isEncrypted(message)) {
            EncryptionResult(ENCRYPTION_TYPE, 0)
        } else {
            null
        }
    }

    companion object {
        const val ENCRYPTION_TYPE = "openpgp"

        @JvmStatic
        fun newInstance(): OpenPgpEncryptionExtractor {
            val textPartFinder = TextPartFinder()
            val encryptionDetector = EncryptionDetector(textPartFinder)
            return OpenPgpEncryptionExtractor(encryptionDetector)
        }
    }
}
