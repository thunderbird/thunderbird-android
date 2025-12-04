package net.thunderbird.feature.mail.message.reader.api.css

/**
 * Provides CSS class names used for styling the message viewer.
 *
 * This allows for a consistent and centralized way to manage the class names
 * used in the HTML of the message display, making it easier to style and
 * maintain.
 */
interface CssClassNameProvider {
    /**
     * The class name used to namespace all CSS rules to avoid conflicts with message content.
     *
     * This class should be applied to a high-level container element wrapping the entire message view.
     */
    val defaultNamespaceClassName: String

    /**
     * The class name for the root element of the message content. This is typically the `<body>` tag.
     */
    val rootClassName: String

    /**
     * The class name for the main content block of the message viewer.
     */
    val mainContentClassName: String

    /**
     * The class name for the `<pre>` tag that wraps plain text messages to preserve formatting.
     */
    val plainTextMessagePreClassName: String

    /**
     * The class name used to style email signatures.
     */
    val signatureClassName: String
}
