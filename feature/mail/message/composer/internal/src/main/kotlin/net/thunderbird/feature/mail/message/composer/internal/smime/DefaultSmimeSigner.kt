package net.thunderbird.feature.mail.message.composer.internal.smime

import java.io.ByteArrayOutputStream
import net.thunderbird.components.core.logging.Logger
import net.thunderbird.feature.mail.message.composer.smime.SmimeKeyChainProvider
import net.thunderbird.feature.mail.message.composer.smime.SmimeSignatureResult
import net.thunderbird.feature.mail.message.composer.smime.SmimeSigner
import net.thunderbird.feature.mail.message.composer.smime.SmimeSigningException

internal class DefaultSmimeSigner(
    private val keyChainProvider: SmimeKeyChainProvider,
    private val cmsSignedDataBuilder: CmsSignedDataBuilder,
    private val logger: Logger,
) : SmimeSigner {

    @Throws(SmimeSigningException::class)
    override fun sign(canonicalMimeContent: ByteArray, certificateAlias: String): SmimeSignatureResult {
        logger.debug(tag = TAG) { "Signing outgoing MIME payload with S/MIME" }
        val credentials = keyChainProvider.getSigningCredentials(certificateAlias)
        val detachedSignature = cmsSignedDataBuilder.buildDetachedSignature(
            canonicalMimeContent = canonicalMimeContent,
            credentials = credentials,
        )
        return SmimeSignatureResult(
            derSignature = detachedSignature.derBytes,
            base64Signature = encodeMimeBase64(detachedSignature.derBytes),
            micAlg = detachedSignature.micAlg,
        )
    }

    @Suppress("MagicNumber")
    private fun encodeMimeBase64(input: ByteArray): ByteArray {
        val out = ByteArrayOutputStream(((input.size + 2) / 3) * 4 + (input.size / 57 + 1) * 2)
        var lineLength = 0
        var index = 0

        while (index < input.size) {
            val b0 = input[index].toInt() and 0xFF
            val hasB1 = index + 1 < input.size
            val hasB2 = index + 2 < input.size
            val b1 = if (hasB1) input[index + 1].toInt() and 0xFF else 0
            val b2 = if (hasB2) input[index + 2].toInt() and 0xFF else 0

            val triple = (b0 shl 16) or (b1 shl 8) or b2
            out.write(BASE64_ALPHABET[(triple ushr 18) and 0x3F].code)
            out.write(BASE64_ALPHABET[(triple ushr 12) and 0x3F].code)
            out.write(if (hasB1) BASE64_ALPHABET[(triple ushr 6) and 0x3F].code else '='.code)
            out.write(if (hasB2) BASE64_ALPHABET[triple and 0x3F].code else '='.code)

            lineLength += 4
            if (lineLength >= MIME_BASE64_LINE_LENGTH) {
                out.write('\r'.code)
                out.write('\n'.code)
                lineLength = 0
            }
            index += 3
        }

        if (lineLength > 0) {
            out.write('\r'.code)
            out.write('\n'.code)
        }
        return out.toByteArray()
    }

    private companion object {
        const val TAG = "DefaultSmimeSigner"
        const val MIME_BASE64_LINE_LENGTH = 76
        const val BASE64_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    }
}
