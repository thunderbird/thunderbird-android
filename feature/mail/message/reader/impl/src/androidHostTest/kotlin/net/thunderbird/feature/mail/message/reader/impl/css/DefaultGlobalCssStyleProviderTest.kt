package net.thunderbird.feature.mail.message.reader.impl.css

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import kotlin.test.Test
import net.thunderbird.core.common.mail.html.HtmlSettings
import net.thunderbird.feature.mail.message.reader.api.css.CssClassNameProvider
import net.thunderbird.feature.mail.message.reader.api.css.CssVariableNameProvider

class DefaultGlobalCssStyleProviderTest {

    @Test
    fun `style should advertise light only color scheme when dark mode is enabled`() {
        // Arrange
        val testSubject = createTestSubject()

        // Act
        val style = testSubject.create(createHtmlSettings(useDarkMode = true)).style

        // Assert
        assertThat(style).contains(":root { color-scheme: only light; }")
    }

    @Test
    fun `style should not override color scheme when dark mode is disabled`() {
        // Arrange
        val testSubject = createTestSubject()

        // Act
        val style = testSubject.create(createHtmlSettings(useDarkMode = false)).style

        // Assert
        assertThat(style).doesNotContain("color-scheme")
    }

    @Test
    fun `style should set main content box sizing to border box`() {
        // Arrange
        val testSubject = createTestSubject()

        // Act
        val styles = createStylesForBothThemeConfigurations(testSubject)

        // Assert
        styles.forEach { style ->
            assertThat(style).contains("box-sizing: border-box")
        }
    }

    @Test
    fun `style should preserve main content width and padding`() {
        // Arrange
        val testSubject = createTestSubject()

        // Act
        val styles = createStylesForBothThemeConfigurations(testSubject)

        // Assert
        styles.forEach { style ->
            val mainContentRule = style.mainContentRule()

            assertThat(mainContentRule).contains("width: 100%")
            assertThat(mainContentRule).contains("overflow-wrap: break-word")
            assertThat(mainContentRule).contains("padding: 0 8px")
        }
    }

    @Test
    fun `style should preserve pre wrapping`() {
        // Arrange
        val testSubject = createTestSubject()

        // Act
        val styles = createStylesForBothThemeConfigurations(testSubject)

        // Assert
        styles.forEach { style ->
            assertThat(style).contains("white-space: pre-wrap")
        }
    }

    @Test
    fun `style should preserve selectable content`() {
        // Arrange
        val testSubject = createTestSubject()

        // Act
        val styles = createStylesForBothThemeConfigurations(testSubject)

        // Assert
        styles.forEach { style ->
            assertThat(style).contains("\n    user-select: auto;\n")
            assertThat(style).contains("-webkit-user-select: auto")
        }
    }

    @Test
    fun `style should preserve blockquote styling`() {
        // Arrange
        val testSubject = createTestSubject()

        // Act
        val styles = createStylesForBothThemeConfigurations(testSubject)

        // Assert
        styles.forEach { style ->
            assertThat(style).contains("margin: auto 0 auto 0.8ex !important")
            assertThat(style).contains("padding-left: 1ex !important")
            assertThat(style).contains("border-left-width: 1px !important")
            assertThat(style).contains("border-left-style: solid !important")
            assertThat(style).contains("border-left-color: var(--blockquote-default-border-left-color, #ccc)")
        }
    }

    private fun createTestSubject(): DefaultGlobalCssStyleProvider.Factory {
        return DefaultGlobalCssStyleProvider.Factory(
            cssClassNameProvider = FakeCssClassNameProvider,
            cssVariableNameProvider = FakeCssVariableNameProvider,
        )
    }

    private fun createStylesForBothThemeConfigurations(
        testSubject: DefaultGlobalCssStyleProvider.Factory,
    ): List<String> {
        return listOf(
            testSubject.create(createHtmlSettings(useDarkMode = false)).style,
            testSubject.create(createHtmlSettings(useDarkMode = true)).style,
        )
    }

    private fun createHtmlSettings(useDarkMode: Boolean): HtmlSettings {
        return HtmlSettings(
            useDarkMode = useDarkMode,
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
