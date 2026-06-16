package com.fsck.k9.view

import android.view.View
import assertk.assertThat
import assertk.assertions.isEqualTo
import net.thunderbird.core.android.testing.RobolectricTest
import org.junit.Test
import org.robolectric.RuntimeEnvironment

class MessageWebViewTest : RobolectricTest() {
    private val context = RuntimeEnvironment.getApplication()

    @Test
    fun configure_shouldNotUseViewSpecificHardwareLayer() {
        val testSubject = MessageWebView(context).apply {
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }

        try {
            testSubject.configure(
                WebViewConfig(
                    useDarkMode = false,
                    autoFitWidth = false,
                    textZoom = 100,
                ),
            )
        } catch (_: UnsupportedOperationException) {
            // Robolectric's WebView provider can reject the AndroidX dark-mode WebSettings API.
        }

        assertThat(testSubject.layerType).isEqualTo(View.LAYER_TYPE_NONE)
    }
}
