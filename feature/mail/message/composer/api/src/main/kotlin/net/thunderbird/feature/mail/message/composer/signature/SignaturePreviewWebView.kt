@file:JvmName("SignaturePreviewWebView")

package net.thunderbird.feature.mail.message.composer.signature

import net.thunderbird.core.android.webkit.ThunderbirdWebViewSettings
import net.thunderbird.core.android.webkit.WebViewConfig

/**
 * Configures a WebView to preview the user's own HTML signature while composing a message.
 *
 * [config] carries the shared composer defaults (dark mode, auto-fit width, text zoom). The two
 * settings applied on top of it deviate from the received-mail defaults, because a signature
 * preview shows content the user authored rather than content that arrived from a stranger:
 *
 * - Network data is unblocked as the remote images in a signature is provided by the user and not
 *   coming from an external factor.
 * - Overview mode is disabled. It scales a page down until its widest content fits the screen, so a
 *   signature holding a wide table or `<pre>` block would render the whole preview zoomed out while
 *   the rest of the composer stays at natural size.
 */
fun ThunderbirdWebViewSettings.configureForSignaturePreview(config: WebViewConfig) {
    configure(config)
    blockNetworkData(shouldBlockNetworkData = false)
    loadWithOverviewMode = false
}
