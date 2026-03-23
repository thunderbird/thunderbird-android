package net.thunderbird.feature.mail.message.list.ui.component.organism

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import net.thunderbird.core.preference.display.visualSettings.message.list.UiDensity
import net.thunderbird.core.ui.compose.theme2.MainTheme

/**
 * Contains the default values used by all [MessageItem] types.
 */
object MessageItemDefaults {
    const val ATTACHMENT_ICON_INLINE_COMPOSABLE_ID = "attachment_icon_inline_composable_id"
    const val ATTACHMENT_ICON_INLINE_COMPOSABLE_REPLACEMENT = "[attachment_icon]"
    const val CONVERSATION_COUNTER_INLINE_COMPOSABLE_ID = "conversation_counter_inline_composable_id"
    const val CONVERSATION_COUNTER_INLINE_COMPOSABLE_REPLACEMENT = "[conversation_counter]"

    /**
     * The default content padding.
     */
    val defaultContentPadding: PaddingValues
        @Composable
        @ReadOnlyComposable
        get() = PaddingValues(
            top = MainTheme.spacings.oneHalf,
            bottom = MainTheme.spacings.oneHalf,
            start = MainTheme.spacings.triple,
        )

    /**
     * The compact mode content padding. This provides a smaller content padding for the [MessageItem],
     * suitable for users who prefer less spacing.
     */
    val compactContentPadding: PaddingValues
        @Composable
        @ReadOnlyComposable
        get() = PaddingValues(
            top = MainTheme.spacings.default,
            bottom = MainTheme.spacings.default,
            start = MainTheme.spacings.double,
        )

    /**
     * The relaxed mode content padding. This provides a larger content padding for the [MessageItem],
     * suitable for users who prefer more spacing.
     */
    val relaxedContentPadding: PaddingValues
        @Composable
        @ReadOnlyComposable
        get() = PaddingValues(
            top = MainTheme.spacings.double,
            bottom = MainTheme.spacings.double,
            start = MainTheme.spacings.quadruple,
        )

    /**
     * Creates a [MessageItemColors] that represent a new message item.
     *
     * This is typically used to highlight a message that has just arrived.
     *
     * @param containerColor The container color of this [MessageItem].
     * @param contentColor The content color of this [MessageItem].
     */
    @Composable
    fun newMessageItemColors(
        containerColor: Color = MainTheme.colors.surfaceContainerLowest,
        contentColor: Color = MainTheme.colors.onSurface,
    ): MessageItemColors = MessageItemColors(
        containerColor = containerColor,
        contentColor = contentColor,
    )

    /**
     * Creates a [MessageItemColors] that represent an unread message item.
     *
     * This is typically used to highlight a message that is unread.
     *
     * @param containerColor The container color of this [MessageItem].
     * @param contentColor The content color of this [MessageItem].
     *  Defaults to [contentColor] if not specified.
     */
    @Composable
    fun defaultMessageItemColors(
        containerColor: Color = MainTheme.colors.surfaceContainerLowest,
        contentColor: Color = MainTheme.colors.onSurface,
    ): MessageItemColors = MessageItemColors(
        containerColor = containerColor,
        contentColor = contentColor,
    )

    /**
     * Creates a [MessageItemColors] that represent a read message item.
     *
     * This is typically used to highlight a message that is read.
     *
     * @param containerColor The container color of this [MessageItem].
     * @param contentColor The content color of this [MessageItem].
     *  Defaults to [contentColor] if not specified.
     */
    @Composable
    fun readMessageItemColors(
        containerColor: Color = MainTheme.colors.surfaceContainerLow,
        contentColor: Color = MainTheme.colors.onSurfaceVariant,
    ): MessageItemColors = MessageItemColors(
        containerColor = containerColor,
        contentColor = contentColor,
    )

    /**
     * Creates a [MessageItemColors] that represent a message item that was selected.
     *
     * This is typically used to highlight a selected message.
     *
     * @param containerColor The container color of this [MessageItem].
     * @param contentColor The content color of this [MessageItem].
     */
    @Composable
    fun selectedMessageItemColors(
        containerColor: Color = MainTheme.colors.surfaceContainer,
        contentColor: Color = MainTheme.colors.onSurface,
    ): MessageItemColors = MessageItemColors(
        containerColor = containerColor,
        contentColor = contentColor,
    )

    /**
     * Creates a [MessageItemColors] that represent a message item that is currently active.
     *
     * This is typically used to highlight the currently displayed message in a split view.
     *
     * @param containerColor The container color of this [MessageItem].
     * @param contentColor The content color of this [MessageItem].
     */
    @Composable
    fun activeMessageItemColors(
        // MainTheme.colors.infoContainer == MainTheme.colors.surfaceVariantBlue
        containerColor: Color = MainTheme.colors.infoContainer,
        contentColor: Color = MainTheme.colors.onSurface,
    ): MessageItemColors = MessageItemColors(
        containerColor = containerColor,
        contentColor = contentColor,
    )

    /**
     * Converts a UiDensity value to its corresponding [PaddingValues] for content
     * spacing.
     *
     * Maps each density level to predefined padding values that control the spacing
     * around content elements.
     *
     * @return [PaddingValues] representing the appropriate content padding for the
     * current density level.
     */
    @Composable
    internal fun UiDensity.toContentPadding(): PaddingValues = when (this) {
        UiDensity.Compact -> compactContentPadding
        UiDensity.Default -> defaultContentPadding
        UiDensity.Relaxed -> relaxedContentPadding
    }

    internal fun buildSubjectAnnotatedString(subject: String): AnnotatedString = buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(subject)
        }
    }
}

/**
 * Represents the colors used by a [MessageItem].
 *
 * @param containerColor The color used for the background of this message item.
 * @param contentColor The preferred color for content inside this message item.
 */
data class MessageItemColors(
    val containerColor: Color,
    val contentColor: Color,
)
