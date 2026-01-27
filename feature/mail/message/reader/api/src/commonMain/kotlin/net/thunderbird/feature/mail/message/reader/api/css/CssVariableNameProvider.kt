package net.thunderbird.feature.mail.message.reader.api.css

/**
 * Provides CSS variable values for theming the message viewer.
 *
 * Implementations of this interface supply the name of the variables (e.g., "--my-variable")
 * that will be used in the message display's stylesheet.
 */
interface CssVariableNameProvider {
    /**
     * The name of the CSS variable used to specify the 'border-left-color' for blockquote elements.
     */
    val blockquoteDefaultBorderLeftColor: String
}
