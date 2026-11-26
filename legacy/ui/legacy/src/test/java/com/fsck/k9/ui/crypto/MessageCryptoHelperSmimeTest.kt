package com.fsck.k9.ui.crypto

import android.content.IntentSender
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.fsck.k9.K9RobolectricTest
import com.fsck.k9.autocrypt.AutocryptOperations
import com.fsck.k9.crypto.MessageCryptoStructureDetector
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.internet.MimeBodyPart
import com.fsck.k9.mail.internet.MimeHeader
import com.fsck.k9.mail.internet.MimeMessage
import com.fsck.k9.mail.internet.MimeMessageHelper
import com.fsck.k9.mail.internet.MimeMultipart
import com.fsck.k9.mail.internet.TextBody
import com.fsck.k9.mailstore.BinaryMemoryBody
import com.fsck.k9.mailstore.CryptoResultAnnotation
import com.fsck.k9.mailstore.CryptoResultAnnotation.CryptoError
import com.fsck.k9.mailstore.MessageCryptoAnnotations
import com.fsck.k9.view.MessageCryptoDisplayStatus
import net.thunderbird.components.core.logging.testing.TestLogger
import net.thunderbird.core.android.testing.RobolectricPendingWorkRule
import net.thunderbird.feature.mail.message.composer.smime.SmimeVerificationResult
import net.thunderbird.feature.mail.message.composer.smime.SmimeVerificationStatus
import net.thunderbird.feature.mail.message.composer.smime.SmimeVerifier
import net.thunderbird.legacy.logging.Log
import org.apache.james.mime4j.util.MimeUtil
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.RuntimeEnvironment

class MessageCryptoHelperSmimeTest : K9RobolectricTest() {

    @get:Rule
    val pendingWork = RobolectricPendingWorkRule()

    @Before
    fun setUp() {
        Log.logger = TestLogger()
    }

    @Test
    fun `smime signed message should be verified without OpenPGP provider even when hideSignOnly is true`() {
        // Arrange
        val message = createSmimeSignedMessage(
            protocol = "application/pkcs7-signature",
            signatureBody = "MAA=\r\n".toByteArray(Charsets.US_ASCII),
        )
        val fakeVerifier = FakeSmimeVerifier(
            result = SmimeVerificationResult(
                status = SmimeVerificationStatus.VALID_TRUSTED,
                signerDisplayName = "Alice Example",
                signerEmail = "alice@example.com",
                issuerDisplayName = "Example CA",
            ),
        )
        val testSubject = MessageCryptoHelper(
            RuntimeEnvironment.getApplication(),
            OpenPgpApiFactory(),
            AutocryptOperations.getInstance(),
            null,
            fakeVerifier,
        )
        val callback = RecordingCryptoCallback()

        // Act
        testSubject.asyncStartOrResumeProcessingMessage(message, callback, null, false)
        pendingWork.runAllTasks()

        // Assert
        assertThat(MessageCryptoStructureDetector.hasSmimeSignedPart(message)).isTrue()
        assertThat(fakeVerifier.invocationCount).isEqualTo(1)
        assertThat(fakeVerifier.lastSenderEmail).isEqualTo("alice@example.com")

        val annotation = callback.annotations?.get(message)
        assertThat(annotation).isNotNull()
        assertThat(annotation!!.errorType).isEqualTo(CryptoError.SMIME_OK)
        assertThat(annotation.isSmimeResult).isTrue()
        assertThat(annotation.smimeVerificationResult?.status).isEqualTo(SmimeVerificationStatus.VALID_TRUSTED)
        assertThat(MessageCryptoDisplayStatus.fromResultAnnotation(annotation))
            .isEqualTo(MessageCryptoDisplayStatus.SMIME_SIGN_VERIFIED)
    }

    @Test
    fun `smime x-pkcs7-signature protocol should be detected and verified`() {
        // Arrange
        val message = createSmimeSignedMessage(
            protocol = "application/x-pkcs7-signature",
            signatureBody = "MAA=\r\n".toByteArray(Charsets.US_ASCII),
        )
        val fakeVerifier = FakeSmimeVerifier(
            result = SmimeVerificationResult(
                status = SmimeVerificationStatus.VALID_UNTRUSTED,
                signerDisplayName = "Self Signed",
            ),
        )
        val testSubject = MessageCryptoHelper(
            RuntimeEnvironment.getApplication(),
            OpenPgpApiFactory(),
            AutocryptOperations.getInstance(),
            null,
            fakeVerifier,
        )
        val callback = RecordingCryptoCallback()

        // Act
        testSubject.asyncStartOrResumeProcessingMessage(message, callback, null, false)
        pendingWork.runAllTasks()

        // Assert
        val annotation = callback.annotations?.get(message)
        assertThat(annotation).isNotNull()
        assertThat(MessageCryptoDisplayStatus.fromResultAnnotation(annotation))
            .isEqualTo(MessageCryptoDisplayStatus.SMIME_SIGN_UNTRUSTED)
    }

    @Test
    fun `display status should map all S_MIME verification states`() {
        assertThat(
            MessageCryptoDisplayStatus.fromResultAnnotation(
                CryptoResultAnnotation.createSmimeResultAnnotation(
                    SmimeVerificationResult(SmimeVerificationStatus.VALID_TRUSTED),
                    null,
                ),
            ),
        ).isEqualTo(MessageCryptoDisplayStatus.SMIME_SIGN_VERIFIED)

        assertThat(
            MessageCryptoDisplayStatus.fromResultAnnotation(
                CryptoResultAnnotation.createSmimeResultAnnotation(
                    SmimeVerificationResult(SmimeVerificationStatus.VALID_UNTRUSTED),
                    null,
                ),
            ),
        ).isEqualTo(MessageCryptoDisplayStatus.SMIME_SIGN_UNTRUSTED)

        assertThat(
            MessageCryptoDisplayStatus.fromResultAnnotation(
                CryptoResultAnnotation.createSmimeResultAnnotation(
                    SmimeVerificationResult(SmimeVerificationStatus.VALID_SENDER_MISMATCH),
                    null,
                ),
            ),
        ).isEqualTo(MessageCryptoDisplayStatus.SMIME_SIGN_MISMATCH)

        assertThat(
            MessageCryptoDisplayStatus.fromResultAnnotation(
                CryptoResultAnnotation.createSmimeResultAnnotation(
                    SmimeVerificationResult(SmimeVerificationStatus.VALID_EXPIRED),
                    null,
                ),
            ),
        ).isEqualTo(MessageCryptoDisplayStatus.SMIME_SIGN_EXPIRED)

        assertThat(
            MessageCryptoDisplayStatus.fromResultAnnotation(
                CryptoResultAnnotation.createSmimeResultAnnotation(
                    SmimeVerificationResult(SmimeVerificationStatus.INVALID_SIGNATURE),
                    null,
                ),
            ),
        ).isEqualTo(MessageCryptoDisplayStatus.SMIME_SIGN_ERROR)

        assertThat(
            MessageCryptoDisplayStatus.fromResultAnnotation(
                CryptoResultAnnotation.createErrorAnnotation(CryptoError.SMIME_SIGNED_BUT_INCOMPLETE, null),
            ),
        ).isEqualTo(MessageCryptoDisplayStatus.SMIME_SIGN_INCOMPLETE)
    }

    @Test
    fun `hasSmimeSignedPart should return false for plain message`() {
        val plainMessage = MimeMessage().apply {
            MimeMessageHelper.setBody(this, TextBody("plain email"))
        }
        assertThat(MessageCryptoStructureDetector.hasSmimeSignedPart(plainMessage)).isFalse()
    }

    private fun createSmimeSignedMessage(protocol: String, signatureBody: ByteArray): MimeMessage {
        val message = MimeMessage().apply {
            uid = "test-uid"
            setFrom(Address("alice@example.com", "Alice Example"))
        }
        val signedPart = MimeBodyPart.create(TextBody("Hello signed world"), "text/plain")
        val sigPart = MimeBodyPart.create(BinaryMemoryBody(signatureBody, MimeUtil.ENC_BASE64), protocol)
        val multipart = MimeMultipart("multipart/signed", "----smime-boundary").apply {
            addBodyPart(signedPart)
            addBodyPart(sigPart)
        }
        MimeMessageHelper.setBody(message, multipart)
        message.setHeader(
            MimeHeader.HEADER_CONTENT_TYPE,
            "multipart/signed; boundary=\"----smime-boundary\"; protocol=\"$protocol\"; micalg=\"sha-256\"",
        )
        return message
    }

    private class FakeSmimeVerifier(
        private val result: SmimeVerificationResult,
    ) : SmimeVerifier {
        var invocationCount: Int = 0
        var lastSenderEmail: String? = null

        override fun verify(
            canonicalMimeContent: ByteArray,
            signatureBytes: ByteArray,
            senderEmail: String?,
        ): SmimeVerificationResult {
            invocationCount++
            lastSenderEmail = senderEmail
            return result
        }
    }

    private class RecordingCryptoCallback : MessageCryptoCallback {
        var annotations: MessageCryptoAnnotations? = null

        override fun onCryptoHelperProgress(current: Int, max: Int) = Unit

        override fun onCryptoOperationsFinished(annotations: MessageCryptoAnnotations) {
            this.annotations = annotations
        }

        override fun startPendingIntentForCryptoHelper(intentSender: IntentSender, requestCode: Int): Boolean = false
    }
}
