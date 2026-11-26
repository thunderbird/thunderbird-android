package com.fsck.k9.message

import android.content.Intent
import androidx.annotation.VisibleForTesting
import app.k9mail.legacy.di.DI
import com.fsck.k9.CoreResourceProvider
import com.fsck.k9.mail.BoundaryGenerator
import com.fsck.k9.mail.filter.EOLConvertingOutputStream
import com.fsck.k9.mail.internet.MessageIdGenerator
import com.fsck.k9.mail.internet.MimeBodyPart
import com.fsck.k9.mail.internet.MimeHeader
import com.fsck.k9.mail.internet.MimeMessage
import com.fsck.k9.mail.internet.MimeMessageHelper
import com.fsck.k9.mailstore.BinaryMemoryBody
import java.io.ByteArrayOutputStream
import java.io.IOException
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.feature.mail.message.composer.signature.HtmlSignatureSanitizer
import net.thunderbird.feature.mail.message.composer.smime.SmimeSigner
import net.thunderbird.feature.mail.message.composer.smime.SmimeSigningException
import org.apache.james.mime4j.util.MimeUtil

/**
 * Builds an RFC 8551 / RFC 5652 S/MIME `multipart/signed` message with a detached
 * `application/pkcs7-signature` signature created using an Android KeyChain certificate.
 */
class SmimeMessageBuilder @VisibleForTesting internal constructor(
    messageIdGenerator: MessageIdGenerator,
    boundaryGenerator: BoundaryGenerator,
    resourceProvider: CoreResourceProvider,
    settingsManager: GeneralSettingsManager,
    htmlSignatureSanitizer: HtmlSignatureSanitizer,
    private val smimeSigner: SmimeSigner,
) : MessageBuilder(
    messageIdGenerator,
    boundaryGenerator,
    resourceProvider,
    settingsManager,
    htmlSignatureSanitizer,
) {
    private var certificateAlias: String? = null

    fun setCertificateAlias(certificateAlias: String?): SmimeMessageBuilder {
        this.certificateAlias = certificateAlias
        return this
    }

    override fun buildMessageInternal() {
        try {
            val message = build()
            val alias = certificateAlias

            if (isDraft || alias.isNullOrBlank()) {
                queueMessageBuildSuccess(message)
                return
            }

            signMessage(message, alias)
            queueMessageBuildSuccess(message)
        } catch (e: SmimeSigningException) {
            queueMessageBuildException(MessagingException(e.message ?: "S/MIME signing failed", e))
        } catch (e: IOException) {
            queueMessageBuildException(MessagingException("Failed to serialize message for S/MIME signing", e))
        } catch (e: MessagingException) {
            queueMessageBuildException(e)
        }
    }

    override fun buildMessageOnActivityResult(requestCode: Int, data: Intent?) {
        throw UnsupportedOperationException("SmimeMessageBuilder does not handle activity results")
    }

    @Throws(MessagingException::class, IOException::class, SmimeSigningException::class)
    private fun signMessage(message: MimeMessage, alias: String) {
        val signedBodyPart = createBodyPartFromMessageContent(message)
        val canonicalContent = serializeCanonicalMimeEntity(signedBodyPart)
        val signatureResult = smimeSigner.sign(canonicalContent, alias)

        val multipartSigned = createMimeMultipart().apply {
            setSubType("signed")
            addBodyPart(signedBodyPart)
            addBodyPart(createSignatureBodyPart(signatureResult.base64Signature))
        }

        MimeMessageHelper.setBody(message, multipartSigned)

        val contentType = "multipart/signed; boundary=\"${multipartSigned.boundary}\";\r\n" +
            "  protocol=\"$PKCS7_SIGNATURE_MIME_TYPE\"; micalg=\"${signatureResult.micAlg}\""
        message.setHeader(MimeHeader.HEADER_CONTENT_TYPE, contentType)
    }

    @Throws(MessagingException::class)
    private fun createBodyPartFromMessageContent(message: MimeMessage): MimeBodyPart {
        val bodyPart = message.toBodyPart()
        val contentType = message.getHeader(MimeHeader.HEADER_CONTENT_TYPE)
        if (contentType.isNotEmpty()) {
            bodyPart.setHeader(MimeHeader.HEADER_CONTENT_TYPE, contentType[0])
        }
        return bodyPart
    }

    @Throws(MessagingException::class, IOException::class)
    private fun serializeCanonicalMimeEntity(bodyPart: MimeBodyPart): ByteArray {
        val rawBuffer = ByteArrayOutputStream()
        EOLConvertingOutputStream(rawBuffer).use { eolOut ->
            bodyPart.writeTo(eolOut)
            eolOut.flush()
        }
        return rawBuffer.toByteArray()
    }

    @Throws(MessagingException::class)
    private fun createSignatureBodyPart(base64Signature: ByteArray): MimeBodyPart {
        val signatureBody = BinaryMemoryBody(base64Signature, MimeUtil.ENC_BASE64)
        return MimeBodyPart.create(
            signatureBody,
            "$PKCS7_SIGNATURE_MIME_TYPE; name=\"$SIGNATURE_FILENAME\"",
        ).apply {
            addHeader(MimeHeader.HEADER_CONTENT_DISPOSITION, "attachment; filename=\"$SIGNATURE_FILENAME\"")
            addHeader("Content-Description", SIGNATURE_DESCRIPTION)
        }
    }

    companion object {
        private const val PKCS7_SIGNATURE_MIME_TYPE = "application/pkcs7-signature"
        private const val SIGNATURE_FILENAME = "smime.p7s"
        private const val SIGNATURE_DESCRIPTION = "S/MIME Cryptographic Signature"

        @JvmStatic
        fun newInstance(): SmimeMessageBuilder {
            return SmimeMessageBuilder(
                messageIdGenerator = MessageIdGenerator.getInstance(),
                boundaryGenerator = BoundaryGenerator.getInstance(),
                resourceProvider = DI.get(CoreResourceProvider::class.java),
                settingsManager = DI.get(GeneralSettingsManager::class.java),
                htmlSignatureSanitizer = DI.get(HtmlSignatureSanitizer::class.java),
                smimeSigner = DI.get(SmimeSigner::class.java),
            )
        }
    }
}
