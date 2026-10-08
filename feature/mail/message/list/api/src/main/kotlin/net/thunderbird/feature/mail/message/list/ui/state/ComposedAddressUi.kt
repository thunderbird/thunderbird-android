package net.thunderbird.feature.mail.message.list.ui.state

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import net.thunderbird.feature.mail.message.list.ui.state.ComposedAddressStyle.Companion.styles

/**
 * Represents a composable UI representation of an email address with visual styling.
 *
 * This data class encapsulates all the information needed to display an email address
 * in the UI, including the display name, text styling information, an optional avatar,
 * and an optional color for visual theming or identification.
 *
 * @property displayName The text representation of the address to be displayed in the UI.
 * @property displayNameStyles A list of style spans to apply to the display name, such as bold
 *  or regular text styling. Each style defines a range within the display name text.
 *  Defaults to an empty list.
 * @property avatar The avatar representation for the address. Can be a monogram, image, or icon.
 *  `null` if no avatar should be displayed.
 * @property color An optional color associated with the address, typically used for visual
 *  theming or identification purposes. `null` if no specific color is assigned.
 */
@Immutable
data class ComposedAddressUi(
    val displayName: String,
    val displayNameStyles: ImmutableList<ComposedAddressStyle> = persistentListOf(),
    val avatar: Avatar? = null,
    val color: Color? = null,
)

/**
 * Defines styling information for composed email addresses in the message list UI.
 *
 * This sealed interface represents different text styles that can be applied to segments
 * of an email address string. Each style defines a range within the address string using
 * start and optional end positions.
 *
 * The interface is marked as Immutable to indicate that implementations should not change
 * after creation, making them safe to use in the Compose UI state.
 */
@Immutable
sealed interface ComposedAddressStyle {
    val start: Int
    val end: Int?

    /**
     * Represents a bold text style to be applied to a segment of an email address string.
     *
     * @property start The starting index (inclusive) where the bold style begins within the address string.
     * @property end The ending index (exclusive) where the bold style ends, or `null` to extend to the
     *  end of the string.
     */
    data class Bold(override val start: Int, override val end: Int? = null) : ComposedAddressStyle

    /**
     * Represents a regular (non-bold, default) text style to be applied to a segment of an email address string.
     *
     * @property start The starting index (inclusive) where the regular style begins within the address string.
     * @property end The ending index (exclusive) where the regular style ends, or `null` to extend to the
     *  end of the string.
     */
    data class Regular(override val start: Int, override val end: Int? = null) : ComposedAddressStyle

    companion object {
        /**
         * A predefined style configuration that applies bold formatting to an entire email address string.
         *
         * This constant provides a convenient way to style the complete address text as bold by creating
         * a single Bold style starting at position 0 with no defined end, meaning the bold formatting
         * extends to the end of the string.
         */
        val AllBold = persistentListOf(Bold(start = 0))

        /**
         * Creates an immutable list of [ComposedAddressStyle] instances from the provided vararg parameters.
         *
         * This function is a convenience method for constructing an immutable list of address styles
         * that can be applied to composed addresses in the message list UI.
         *
         * @param styles Variable number of [ComposedAddressStyle] instances to be combined into an immutable list.
         * @return An immutable list containing all the provided [ComposedAddressStyle] instances.
         */
        fun styles(vararg styles: ComposedAddressStyle): ImmutableList<ComposedAddressStyle> = styles.toImmutableList()
    }
}
