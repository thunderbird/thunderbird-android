package net.thunderbird.feature.mail.message.reader.api.css

/**
 * Provides CSS styles to be injected into the message viewer's web view.
 *
 * This sealed interface serves as a common type for different CSS style providers.
 * Each implementation is responsible for a specific part of the message styling,
 * such as global styles, signature styles, or plain text formatting.
 */
sealed interface CssStyleProvider {
    /**
     * The CSS style tag with its content as a string.
     *
     * This string should contain valid CSS rules that can be injected into an HTML document (e.g., a message view).
     *
     * Example:
     * ```
     * "body { color: red; } a { text-decoration: none; }"
     * ```
     */
    val style: String
}

/**
 * Provides CSS styles that are applied to the entire message view.
 *
 * This is used for global styles that affect the overall appearance of the email content,
 * such as font size, colors, and layout adjustments for the entire message body.
 */
interface GlobalCssStyleProvider : CssStyleProvider

/**
 * Provides CSS styles specifically for the `<pre>` element used to wrap plain text messages.
 *
 * This allows for custom styling of plain text content, such as setting the font family,
 * size, and controlling word wrapping behavior to ensure readability.
 */
interface PlainTextMessagePreElementCssStyleProvider : CssStyleProvider {
    interface Factory {
        fun create(useFixedWidthFont: Boolean): PlainTextMessagePreElementCssStyleProvider
    }
}

/**
 * Provides CSS styles specifically for email signatures.
 *
 * This is used to apply custom styling to the signature block within an email message,
 * allowing it to be visually distinct from the main message body.
 */
interface SignatureCssStyleProvider : CssStyleProvider
