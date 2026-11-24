package com.fsck.k9.message

import android.app.PendingIntent
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.fsck.k9.CoreResourceProvider
import com.fsck.k9.TestCoreResourceProvider
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.BoundaryGenerator
import com.fsck.k9.mail.filter.EOLConvertingOutputStream
import com.fsck.k9.mail.internet.MessageIdGenerator
import com.fsck.k9.mail.internet.MimeHeader
import com.fsck.k9.mail.internet.MimeMessage
import com.fsck.k9.mail.internet.MimeMultipart
import java.io.ByteArrayOutputStream
import java.util.Date
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import net.thunderbird.components.core.logging.testing.TestLogger
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.QuoteStyle
import net.thunderbird.core.android.testing.RobolectricPendingWorkRule
import net.thunderbird.core.android.testing.RobolectricTest
import net.thunderbird.core.common.appConfig.PlatformConfigProvider
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.preference.GeneralSettings
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.feature.mail.message.composer.signature.HtmlSignatureSanitizer
import net.thunderbird.feature.mail.message.composer.smime.SmimeSignatureResult
import net.thunderbird.feature.mail.message.composer.smime.SmimeSigner
import net.thunderbird.feature.mail.message.composer.smime.SmimeSigningException
import net.thunderbird.legacy.logging.Log
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SmimeMessageBuilderTest : RobolectricTest() {

    @get:Rule
    val pendingWork = RobolectricPendingWorkRule()

    private val resourceProvider: CoreResourceProvider = TestCoreResourceProvider()

    @Before
    fun setUp() {
        Log.logger = TestLogger()
    }

    @Test
    fun `build plain text message with S_MIME alias should produce multipart signed message`() {
        // Arrange
        val fakeSigner = FakeSmimeSigner(
            result = SmimeSignatureResult(
                derSignature = byteArrayOf(0x30, 0x00),
                base64Signature = "MAA=\r\n".toByteArray(Charsets.US_ASCII),
                micAlg = "sha-256",
            ),
        )
        val testSubject = createBuilder(fakeSigner)
            .setCertificateAlias("work-smime-cert")
        configureCommonFields(testSubject, SimpleMessageFormat.TEXT, isDraft = false)
        val callback = RecordingCallback()

        // Act
        testSubject.buildAsync(callback)
        pendingWork.runAllTasks()

        // Assert
        assertThat(callback.exception).isNull()
        val message = callback.message
        assertThat(message).isNotNull()
        assertThat(fakeSigner.lastAlias).isEqualTo("work-smime-cert")
        assertThat(message!!.mimeType).isEqualTo("multipart/signed")
        assertThat(message.contentType).contains("protocol=\"application/pkcs7-signature\"")
        assertThat(message.contentType).contains("micalg=\"sha-256\"")

        val multipart = message.body as MimeMultipart
        assertThat(multipart.count).isEqualTo(2)

        val signedEntityPart = multipart.getBodyPart(0)
        assertThat(signedEntityPart.mimeType).isEqualTo("text/plain")

        // Verify signed bytes match the serialized first body part identically
        val serializedFirstPart = ByteArrayOutputStream().also { out ->
            EOLConvertingOutputStream(out).use { eolOut ->
                signedEntityPart.writeTo(eolOut)
                eolOut.flush()
            }
        }.toByteArray()
        assertThat(fakeSigner.lastSignedContent).isNotNull().isEqualTo(serializedFirstPart)

        val signaturePart = multipart.getBodyPart(1)
        assertThat(signaturePart.mimeType).isEqualTo("application/pkcs7-signature")
        assertThat(signaturePart.contentType).contains("name=\"smime.p7s\"")
        assertThat(signaturePart.getHeader(MimeHeader.HEADER_CONTENT_TRANSFER_ENCODING).first()).isEqualTo("base64")
        assertThat(signaturePart.disposition).isEqualTo("attachment; filename=\"smime.p7s\"")
        assertThat(signaturePart.getHeader("Content-Description").first()).isEqualTo("S/MIME Cryptographic Signature")
    }

    @Test
    fun `build HTML message with S_MIME alias should wrap multipart alternative in multipart signed`() {
        // Arrange
        val fakeSigner = FakeSmimeSigner(
            result = SmimeSignatureResult(
                derSignature = byteArrayOf(0x30, 0x00),
                base64Signature = "MAA=\r\n".toByteArray(Charsets.US_ASCII),
                micAlg = "sha-384",
            ),
        )
        val testSubject = createBuilder(fakeSigner)
            .setCertificateAlias("ec384-cert")
        configureCommonFields(testSubject, SimpleMessageFormat.HTML, isDraft = false)
        val callback = RecordingCallback()

        // Act
        testSubject.buildAsync(callback)
        pendingWork.runAllTasks()

        // Assert
        assertThat(callback.exception).isNull()
        val message = callback.message!!
        assertThat(message.mimeType).isEqualTo("multipart/signed")
        assertThat(message.contentType).contains("micalg=\"sha-384\"")
        val multipart = message.body as MimeMultipart
        assertThat(multipart.getBodyPart(0).mimeType).isEqualTo("multipart/alternative")
        assertThat(multipart.getBodyPart(1).mimeType).isEqualTo("application/pkcs7-signature")
    }

    @Test
    fun `build draft message should not invoke S_MIME signer`() {
        // Arrange
        val fakeSigner = FakeSmimeSigner(
            result = SmimeSignatureResult(
                derSignature = byteArrayOf(0x30, 0x00),
                base64Signature = "MAA=\r\n".toByteArray(Charsets.US_ASCII),
                micAlg = "sha-256",
            ),
        )
        val testSubject = createBuilder(fakeSigner)
            .setCertificateAlias("work-smime-cert")
        configureCommonFields(testSubject, SimpleMessageFormat.TEXT, isDraft = true)
        val callback = RecordingCallback()

        // Act
        testSubject.buildAsync(callback)
        pendingWork.runAllTasks()

        // Assert
        assertThat(fakeSigner.invocationCount).isEqualTo(0)
        assertThat(callback.message!!.mimeType).isEqualTo("text/plain")
    }

    @Test
    fun `build should deliver MessagingException when S_MIME signing fails`() {
        // Arrange
        val fakeSigner = FakeSmimeSigner(
            failure = SmimeSigningException("Certificate expired"),
        )
        val testSubject = createBuilder(fakeSigner)
            .setCertificateAlias("expired-cert")
        configureCommonFields(testSubject, SimpleMessageFormat.TEXT, isDraft = false)
        val callback = RecordingCallback()

        // Act
        testSubject.buildAsync(callback)
        pendingWork.runAllTasks()

        // Assert
        assertThat(callback.message).isNull()
        assertThat(callback.exception).isNotNull()
        assertThat(callback.exception!!.cause).isNotNull().isInstanceOf<SmimeSigningException>()
    }

    private fun createBuilder(smimeSigner: SmimeSigner): SmimeMessageBuilder {
        return SmimeMessageBuilder(
            messageIdGenerator = MessageIdGenerator.getInstance(),
            boundaryGenerator = BoundaryGenerator.getInstance(),
            resourceProvider = resourceProvider,
            settingsManager = FakeGeneralSettingsManager(),
            htmlSignatureSanitizer = HtmlSignatureSanitizer { it },
            smimeSigner = smimeSigner,
        )
    }

    private fun configureCommonFields(
        builder: MessageBuilder,
        format: SimpleMessageFormat,
        isDraft: Boolean,
    ) {
        builder.setSubject("S/MIME Test Subject")
            .setSentDate(Date(1_767_323_045_000L))
            .setHideTimeZone(true)
            .setTo(listOf(Address("recipient@example.com", "Recipient")))
            .setCc(emptyList())
            .setBcc(emptyList())
            .setIdentity(Identity(name = "Sender", email = "sender@example.com", signatureUse = false))
            .setMessageFormat(format)
            .setText("Hello signed email!")
            .setAttachments(emptyList())
            .setSignature("")
            .setQuoteStyle(QuoteStyle.PREFIX)
            .setQuotedTextMode(QuotedTextMode.NONE)
            .setQuotedText("")
            .setReplyAfterQuote(false)
            .setSignatureBeforeQuotedText(false)
            .setIdentityChanged(false)
            .setSignatureChanged(false)
            .setCursorPosition(0)
            .setDraft(isDraft)
            .setIsPgpInlineEnabled(false)
    }

    private class FakeSmimeSigner(
        private val result: SmimeSignatureResult? = null,
        private val failure: SmimeSigningException? = null,
    ) : SmimeSigner {
        var invocationCount: Int = 0
        var lastAlias: String? = null
        var lastSignedContent: ByteArray? = null

        override fun sign(canonicalMimeContent: ByteArray, certificateAlias: String): SmimeSignatureResult {
            invocationCount++
            lastAlias = certificateAlias
            lastSignedContent = canonicalMimeContent
            failure?.let { throw it }
            return requireNotNull(result)
        }
    }

    private class RecordingCallback : MessageBuilder.Callback {
        var message: MimeMessage? = null
        var exception: MessagingException? = null

        override fun onMessageBuildSuccess(message: MimeMessage, isDraft: Boolean) {
            this.message = message
        }

        override fun onMessageBuildCancel() = Unit

        override fun onMessageBuildException(exception: MessagingException) {
            this.exception = exception
        }

        override fun onMessageBuildReturnPendingIntent(pendingIntent: PendingIntent, requestCode: Int) = Unit
    }

    private class FakeGeneralSettingsManager : GeneralSettingsManager {
        private val settings = GeneralSettings(
            platformConfigProvider = object : PlatformConfigProvider {
                override val isDebug: Boolean = false
            },
        )

        @Deprecated("Use getConfig")
        override fun getSettings(): GeneralSettings = settings

        @Deprecated("Use getConfigFlow")
        override fun getSettingsFlow(): Flow<GeneralSettings> = emptyFlow()

        override fun getConfig(): GeneralSettings = settings

        override fun getConfigFlow(): Flow<GeneralSettings> = emptyFlow()

        override fun save(config: GeneralSettings) = Unit
    }
}
