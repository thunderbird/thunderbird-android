package net.thunderbird.feature.mail.message.reader.impl.css

import assertk.assertThat
import assertk.assertions.contains
import kotlin.test.Test
import net.thunderbird.core.common.mail.html.HtmlSettings
import net.thunderbird.feature.mail.message.reader.api.css.CssClassNameProvider
import net.thunderbird.feature.mail.message.reader.api.css.CssVariableNameProvider

class DefaultGlobalCssStyleProviderTest {

    @Test
    fun `style should set main content box sizing to border box`() {
        // Arrange
        val testSubject = createTestSubject()

        // Act
        val style = testSubject.create(createHtmlSettings()).style

        // Assert
        assertThat(style).contains("box-sizing: border-box")
    }

    @Test
    fun `style should preserve main content width and padding`() {
        // Arrange
        val testSubject = createTestSubject()

        // Act
        val style = testSubject.create(createHtmlSettings()).style
        val mainContentRule = style.mainContentRule()

        // Assert
        assertThat(mainContentRule).contains("width: 100%")
        assertThat(mainContentRule).contains("padding: 0 8px")
    }

    @Test
    fun `style should preserve pre wrapping`() {
        // Arrange
        val testSubject = createTestSubject()

        // Act
        val style = testSubject.create(createHtmlSettings()).style

        // Assert
        assertThat(style).contains("white-space: pre-wrap")
    }

    private fun createTestSubject(): DefaultGlobalCssStyleProvider.Factory {
        return DefaultGlobalCssStyleProvider.Factory(
            cssClassNameProvider = FakeCssClassNameProvider,
            cssVariableNameProvider = FakeCssVariableNameProvider,
        )
    }

    private fun createHtmlSettings(): HtmlSettings {
        return HtmlSettings(
            useDarkMode = false,
            useFixedWidthFont = false,
        )
    }

    private fun String.mainContentRule(): String {
        return substringAfter("  .root.main-content {")
            .substringBefore("  .root.main-content pre {")
    }

    private object FakeCssClassNameProvider : CssClassNameProvider {
        override val defaultNamespaceClassName = "default-namespace"
        override val rootClassName = "root"
        override val mainContentClassName = "main-content"
        override val plainTextMessagePreClassName = "plain-text-message-pre"
        override val signatureClassName = "signature"
    }

    private object FakeCssVariableNameProvider : CssVariableNameProvider {
        override val blockquoteDefaultBorderLeftColor = "--blockquote-default-border-left-color"
    }
}
