package net.thunderbird.feature.mail.message.composer.signature

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import net.thunderbird.core.android.webkit.ThunderbirdWebViewSettings
import net.thunderbird.core.android.webkit.WebViewConfig
import org.junit.Test

class SignaturePreviewWebViewTest {

    @Test
    fun `configureForSignaturePreview should apply the given config`() {
        // Arrange
        val config = WebViewConfig(useDarkMode = true, autoFitWidth = true, textZoom = 120)
        val testSubject = FakeThunderbirdWebViewSettings()

        // Act
        testSubject.configureForSignaturePreview(config)

        // Assert
        assertThat(testSubject.appliedConfig).isEqualTo(config)
    }

    @Test
    fun `configureForSignaturePreview should unblock network data`() {
        // Arrange
        val testSubject = FakeThunderbirdWebViewSettings(isNetworkDataBlocked = true)

        // Act
        testSubject.configureForSignaturePreview(CONFIG)

        // Assert
        assertThat(testSubject.isNetworkDataBlocked).isFalse()
    }

    @Test
    fun `configureForSignaturePreview should disable overview mode`() {
        // Arrange
        val testSubject = FakeThunderbirdWebViewSettings(loadWithOverviewMode = true)

        // Act
        testSubject.configureForSignaturePreview(CONFIG)

        // Assert
        assertThat(testSubject.loadWithOverviewMode).isFalse()
    }

    @Test
    fun `configureForSignaturePreview should override the config auto fit width behavior`() {
        // Arrange
        // The composer config enables auto-fit width, which received mail renders with overview mode.
        val config = CONFIG.copy(autoFitWidth = true)
        val testSubject = FakeThunderbirdWebViewSettings()

        // Act
        testSubject.configureForSignaturePreview(config)

        // Assert
        assertThat(testSubject.appliedConfig).isEqualTo(config)
        assertThat(testSubject.loadWithOverviewMode).isFalse()
    }

    private companion object {
        val CONFIG = WebViewConfig(useDarkMode = false, autoFitWidth = false, textZoom = 100)
    }
}

private class FakeThunderbirdWebViewSettings(
    var isNetworkDataBlocked: Boolean = false,
    override var loadWithOverviewMode: Boolean = false,
) : ThunderbirdWebViewSettings {
    var appliedConfig: WebViewConfig? = null
        private set

    override fun blockNetworkData(shouldBlockNetworkData: Boolean) {
        isNetworkDataBlocked = shouldBlockNetworkData
    }

    override fun configure(config: WebViewConfig) {
        appliedConfig = config
        loadWithOverviewMode = config.autoFitWidth
    }
}
