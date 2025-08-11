package net.thunderbird.core.ui.compose.designsystem.organism.message

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import app.k9mail.core.ui.compose.theme2.MainTheme

/**
 * Contains the default values used by all [MessageItem] types.
 */
object MessageItemDefaults {
    /**
     * The default content padding.
     */
    val defaultContentPadding: PaddingValues
        @Composable
        @ReadOnlyComposable
        get() = PaddingValues(
            top = MainTheme.spacings.oneHalf,
            bottom = MainTheme.spacings.oneHalf,
            start = MainTheme.spacings.double,
            end = MainTheme.spacings.triple,
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
            start = MainTheme.spacings.oneHalf,
            end = MainTheme.spacings.double,
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
            start = MainTheme.spacings.triple,
            end = MainTheme.spacings.quadruple,
        )

    /**
     * Creates a [MessageItemColors] that represent a new message item.
     *
     * This is typically used to highlight a message that has just arrived.
     *
     * @param containerColor The container color of this [MessageItem].
     * @param contentColor The content color of this [MessageItem].
     * @param subjectColor The subject color of this [MessageItem].
     */
    @Composable
    fun newMessageItemColors(
        containerColor: Color = MainTheme.colors.surfaceContainerLowest,
        contentColor: Color = MainTheme.colors.onSurface,
        subjectColor: Color = MainTheme.colors.primary,
    ): MessageItemColors = MessageItemColors(
        containerColor = containerColor,
        contentColor = contentColor,
        subjectColor = subjectColor,
    )

    /**
     * Creates a [MessageItemColors] that represent an unread message item.
     *
     * This is typically used to highlight a message that is unread.
     *
     * @param containerColor The container color of this [MessageItem].
     * @param contentColor The content color of this [MessageItem].
     * @param subjectColor The subject color of this [MessageItem].
     *  Defaults to [contentColor] if not specified.
     */
    @Composable
    fun unreadMessageItemColors(
        containerColor: Color = MainTheme.colors.surfaceContainerLowest,
        contentColor: Color = MainTheme.colors.onSurface,
        subjectColor: Color = contentColor,
    ): MessageItemColors = MessageItemColors(
        containerColor = containerColor,
        contentColor = contentColor,
        subjectColor = subjectColor,
    )

    /**
     * Creates a [MessageItemColors] that represent a read message item.
     *
     * This is typically used to highlight a message that is read.
     *
     * @param containerColor The container color of this [MessageItem].
     * @param contentColor The content color of this [MessageItem].
     * @param subjectColor The subject color of this [MessageItem].
     *  Defaults to [contentColor] if not specified.
     */
    @Composable
    fun readMessageItemColors(
        containerColor: Color = MainTheme.colors.surfaceContainerLow,
        contentColor: Color = MainTheme.colors.onSurfaceVariant,
        subjectColor: Color = contentColor,
    ): MessageItemColors = MessageItemColors(
        containerColor = containerColor,
        contentColor = contentColor,
        subjectColor = subjectColor,
    )

    /**
     * Creates a [MessageItemColors] that represent a message item that was selected.
     *
     * This is typically used to highlight a selected message.
     *
     * @param containerColor The container color of this [MessageItem].
     * @param contentColor The content color of this [MessageItem].
     * @param subjectColor The subject color of this [MessageItem].
     */
    @Composable
    fun selectedMessageItemColors(
        containerColor: Color = MainTheme.colors.infoContainer,
        contentColor: Color = MainTheme.colors.onSurface,
        subjectColor: Color = MainTheme.colors.onSurfaceVariant,
    ): MessageItemColors = MessageItemColors(
        containerColor = containerColor,
        contentColor = contentColor,
        subjectColor = subjectColor,
    )

    /**
     * Creates a [MessageItemColors] that represent a message item that is currently active.
     *
     * This is typically used to highlight the currently displayed message in a split view.
     *
     * @param containerColor The container color of this [MessageItem].
     * @param contentColor The content color of this [MessageItem].
     * @param subjectColor The subject color of this [MessageItem].
     */
    @Composable
    fun activeMessageItemColors(
        containerColor: Color = MainTheme.colors.infoContainer,
        contentColor: Color = MainTheme.colors.onSurface,
        subjectColor: Color = MainTheme.colors.onSurfaceVariant,
    ): MessageItemColors = MessageItemColors(
        containerColor = containerColor,
        contentColor = contentColor,
        subjectColor = subjectColor,
    )
}

/**
 * Represents the colors used by a [MessageItem].
 *
 * @param containerColor The color used for the background of this message item.
 * @param contentColor The preferred color for content inside this message item.
 * @param subjectColor The preferred color for the subject inside this message item.
 */
data class MessageItemColors(
    val containerColor: Color,
    val contentColor: Color,
    val subjectColor: Color,
)
