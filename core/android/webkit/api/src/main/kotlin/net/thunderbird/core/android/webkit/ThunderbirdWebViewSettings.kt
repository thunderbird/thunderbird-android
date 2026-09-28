package net.thunderbird.core.android.webkit

/**
 * Interface for configuring WebView settings in Thunderbird.
 *
 * Provides methods to control network data access, apply WebView configuration,
 * and manage display modes for web content rendering.
 */
interface ThunderbirdWebViewSettings {
    /**
     * Controls whether the WebView loads pages in overview mode.
     *
     * When enabled, the WebView displays the entire page width and scales it to fit the screen.
     * When disabled, the page loads at its original size without automatic scaling.
     */
    var loadWithOverviewMode: Boolean

    /**
     * Controls whether the WebView should block network data loading.
     *
     * When enabled, prevents the WebView from loading resources from the network,
     * restricting content to local or cached resources only.
     *
     * @param shouldBlockNetworkData true to block network data access, false to allow it
     */
    fun blockNetworkData(shouldBlockNetworkData: Boolean)

    /**
     * Applies the specified configuration settings to the WebView.
     *
     * Configures the WebView with dark mode preference, auto-fit width behavior,
     * and text zoom level according to the provided configuration object.
     *
     * @param config the configuration containing dark mode, auto-fit width, and text zoom settings
     */
    fun configure(config: WebViewConfig)
}
