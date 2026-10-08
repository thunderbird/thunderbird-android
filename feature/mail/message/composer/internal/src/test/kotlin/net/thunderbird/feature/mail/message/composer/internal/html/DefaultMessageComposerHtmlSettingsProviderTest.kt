package net.thunderbird.feature.mail.message.composer.internal.html

import assertk.assertThat
import assertk.assertions.isEqualTo
import net.thunderbird.core.ui.theme.api.Theme
import net.thunderbird.core.ui.theme.api.ThemeManager
import org.junit.Test

class DefaultMessageComposerHtmlSettingsProviderTest {

    @Test
    fun `create should enable dark mode when compose theme is dark`() {
        // Arrange
        val testSubject = DefaultMessageComposerHtmlSettingsProvider(
            themeManager = FakeThemeManager(messageComposeTheme = Theme.DARK),
        )

        // Act
        val result = testSubject.create()

        // Assert
        assertThat(result.useDarkMode).isEqualTo(true)
        assertThat(result.useFixedWidthFont).isEqualTo(false)
    }

    @Test
    fun `create should disable dark mode when compose theme is light`() {
        // Arrange
        val testSubject = DefaultMessageComposerHtmlSettingsProvider(
            themeManager = FakeThemeManager(messageComposeTheme = Theme.LIGHT),
        )

        // Act
        val result = testSubject.create()

        // Assert
        assertThat(result.useDarkMode).isEqualTo(false)
        assertThat(result.useFixedWidthFont).isEqualTo(false)
    }
}

private class FakeThemeManager(
    override val messageComposeTheme: Theme,
) : ThemeManager {
    override val appTheme: Theme = Theme.LIGHT
    override val messageViewTheme: Theme = Theme.LIGHT
    override val appThemeResourceId: Int = 0
    override val messageViewThemeResourceId: Int = 0
    override val messageComposeThemeResourceId: Int = 0
    override val dialogThemeResourceId: Int = 0
    override val translucentDialogThemeResourceId: Int = 0
}
