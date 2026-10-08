package com.fsck.k9.message

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import com.fsck.k9.notification.FakePlatformConfigProvider
import kotlinx.coroutines.flow.Flow
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.core.preference.GeneralSettings
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.feature.mail.message.composer.signature.HtmlSignatureSanitizer
import net.thunderbird.legacy.logging.Log
import org.junit.Before
import org.junit.Test

/**
 * Exercises the HTML signature short-circuit added for the HTML signature feature.
 *
 * The existing parameterized [TextBodyBuilderTest] covers the plain-text signature
 * path; these cases add coverage for [TextBodyBuilder.setSignatureIsHtml].
 */
class TextBodyBuilderHtmlSignatureTest {

    @Before
    fun setUp() {
        Log.logger = TestLogger()
    }

    @Test
    fun `buildTextHtml should embed signature when signature is html`() {
        // Arrange
        // language=html
        val htmlSignature = """<p>Sent from <b>Thunderbird</b></p>"""
        val testSubject = createTestSubject(signature = htmlSignature, signatureIsHtml = true)

        // Act
        val result = testSubject.buildTextHtml().rawText

        // Assert
        // The <b> tag is preserved. It would have been escaped to &lt;b&gt; if we had
        // gone through HtmlConverter.textToHtmlFragment().
        // language=html
        assertThat(result).contains("<b>Thunderbird</b>")
        assertThat(result).doesNotContain("&lt;b&gt;")
    }

    @Test
    fun `buildTextHtml should use sanitized signature when signature is html`() {
        // Arrange
        // language=html
        val htmlSignature = """<p>Hi</p><script>alert('xss')</script>"""
        // language=html
        val sanitizedSignature = "<p>Hi</p>"
        val testSubject = createTestSubject(
            signature = htmlSignature,
            signatureIsHtml = true,
            sanitizedSignature = sanitizedSignature,
        )

        // Act
        val result = testSubject.buildTextHtml().rawText

        // Assert
        assertThat(result).contains(sanitizedSignature)
        assertThat(result).doesNotContain("<script>")
        assertThat(result).doesNotContain("alert")
    }

    @Test
    fun `buildTextPlain should convert signature to plain text when signature is html`() {
        // Arrange
        // language=html
        // language=html
        val htmlSignature = """<p>Sent from <b>Thunderbird</b></p>"""
        val testSubject = createTestSubject(signature = htmlSignature, signatureIsHtml = true)

        // Act
        val result = testSubject.buildTextPlain().rawText

        // Assert
        // The HTML tags should be stripped for the plain-text path.
        assertThat(result).contains("Sent from Thunderbird")
        assertThat(result).doesNotContain("<b>")
        assertThat(result).doesNotContain("<p>")
    }

    @Test
    fun `buildTextHtml should convert signature to html when signature is plain text`() {
        // Arrange
        val plainSignature = "-- \r\nAlice"
        val testSubject = createTestSubject(signature = plainSignature, signatureIsHtml = false)

        // Act
        val result = testSubject.buildTextHtml().rawText

        // Assert
        // The plain-text path wraps the signature in a k9mail-signature div.
        assertThat(result).contains("k9mail-signature")
    }

    private fun createTestSubject(
        signature: String,
        signatureIsHtml: Boolean,
        messageContent: String = MESSAGE_CONTENT,
        sanitizedSignature: String = signature,
    ): TextBodyBuilder {
        return TextBodyBuilder(
            messageContent,
            FakeGeneralSettingsManager(),
            FakeHtmlSignatureSanitizer(sanitizedSignature),
        ).apply {
            setAppendSignature(true)
            setIncludeQuotedText(false)
            setSignatureIsHtml(signatureIsHtml)
            setSignature(signature)
        }
    }

    private companion object {
        const val MESSAGE_CONTENT = "hello"
    }
}

private class FakeHtmlSignatureSanitizer(
    private val sanitizedSignature: String,
) : HtmlSignatureSanitizer {
    override fun sanitize(html: String): String = sanitizedSignature
}

private class FakeGeneralSettingsManager(
    private var generalSettings: GeneralSettings = GeneralSettings(
        platformConfigProvider = FakePlatformConfigProvider(),
    ),
) : GeneralSettingsManager {
    @Deprecated("Use PreferenceManager<GeneralSettings>.getConfig() instead")
    override fun getSettings(): GeneralSettings = generalSettings

    @Deprecated("Use PreferenceManager<GeneralSettings>.getConfigFlow() instead")
    override fun getSettingsFlow(): Flow<GeneralSettings> = error("Not implemented")

    override fun save(config: GeneralSettings) {
        generalSettings = config
    }

    override fun getConfig(): GeneralSettings = generalSettings

    override fun getConfigFlow(): Flow<GeneralSettings> = error("Not implemented")
}
