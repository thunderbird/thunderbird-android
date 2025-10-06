package app.k9mail.core.ui.compose.designsystem.atom.card

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import app.k9mail.core.ui.compose.theme2.MainTheme
import androidx.compose.material3.CardDefaults as Material3CardDefaults

/**
 * Contains the default values used by all card types.
 */
object CardDefaults {
    // shape Defaults
    /** Default shape for a card. */
    val shape: Shape
        @Composable get() = Material3CardDefaults.shape

    /** Default shape for an elevated card. */
    val elevatedShape: Shape
        @Composable get() = Material3CardDefaults.elevatedShape

    /** Default shape for an outlined card. */
    val outlinedShape: Shape
        @Composable get() = Material3CardDefaults.outlinedShape

    internal const val DISABLED_ALPHA = 0.38f

    /**
     * Creates a [CardElevation] that will animate between the provided values according to the
     * Material specification for a [Card].
     *
     * @param defaultElevation the elevation used when the [Card] is has no other [Interaction]s.
     * @param pressedElevation the elevation used when the [Card] is pressed.
     * @param focusedElevation the elevation used when the [Card] is focused.
     * @param hoveredElevation the elevation used when the [Card] is hovered.
     * @param draggedElevation the elevation used when the [Card] is dragged.
     */
    @Composable
    fun cardElevation(
        defaultElevation: Dp = MainTheme.elevations.level0,
        pressedElevation: Dp = MainTheme.elevations.level0,
        focusedElevation: Dp = MainTheme.elevations.level0,
        hoveredElevation: Dp = MainTheme.elevations.level1,
        draggedElevation: Dp = MainTheme.elevations.level3,
        disabledElevation: Dp = MainTheme.elevations.level0,
    ): CardElevation = CardElevation.FilledCardElevation(
        defaultElevation = defaultElevation,
        pressedElevation = pressedElevation,
        focusedElevation = focusedElevation,
        hoveredElevation = hoveredElevation,
        draggedElevation = draggedElevation,
        disabledElevation = disabledElevation,
    )

    /**
     * Creates a [CardElevation] that will animate between the provided values according to the
     * Material specification for an [ElevatedCard].
     *
     * @param defaultElevation the elevation used when the [ElevatedCard] is has no other
     *   [Interaction]s.
     * @param pressedElevation the elevation used when the [ElevatedCard] is pressed.
     * @param focusedElevation the elevation used when the [ElevatedCard] is focused.
     * @param hoveredElevation the elevation used when the [ElevatedCard] is hovered.
     * @param draggedElevation the elevation used when the [ElevatedCard] is dragged.
     */
    @Composable
    fun elevatedCardElevation(
        defaultElevation: Dp = MainTheme.elevations.level1,
        pressedElevation: Dp = MainTheme.elevations.level1,
        focusedElevation: Dp = MainTheme.elevations.level1,
        hoveredElevation: Dp = MainTheme.elevations.level2,
        draggedElevation: Dp = MainTheme.elevations.level4,
        disabledElevation: Dp = MainTheme.elevations.level1,
    ): CardElevation = CardElevation.ElevatedCardElevation(
        defaultElevation = defaultElevation,
        pressedElevation = pressedElevation,
        focusedElevation = focusedElevation,
        hoveredElevation = hoveredElevation,
        draggedElevation = draggedElevation,
        disabledElevation = disabledElevation,
    )

    /**
     * Creates a [CardElevation] that will animate between the provided values according to the
     * Material specification for an [OutlinedCard].
     *
     * @param defaultElevation the elevation used when the [OutlinedCard] is has no other
     *   [Interaction]s.
     * @param pressedElevation the elevation used when the [OutlinedCard] is pressed.
     * @param focusedElevation the elevation used when the [OutlinedCard] is focused.
     * @param hoveredElevation the elevation used when the [OutlinedCard] is hovered.
     * @param draggedElevation the elevation used when the [OutlinedCard] is dragged.
     */
    @Composable
    fun outlinedCardElevation(
        defaultElevation: Dp = MainTheme.elevations.level0,
        pressedElevation: Dp = defaultElevation,
        focusedElevation: Dp = defaultElevation,
        hoveredElevation: Dp = defaultElevation,
        draggedElevation: Dp = MainTheme.elevations.level3,
        disabledElevation: Dp = MainTheme.elevations.level0,
    ): CardElevation = CardElevation.OutlinedCardElevation(
        defaultElevation = defaultElevation,
        pressedElevation = pressedElevation,
        focusedElevation = focusedElevation,
        hoveredElevation = hoveredElevation,
        draggedElevation = draggedElevation,
        disabledElevation = disabledElevation,
    )

    /**
     * Creates a [CardColors] that represents the default container and content colors used in a
     * [Card].
     */
    @Composable
    fun cardColors(): CardColors = Material3CardDefaults.cardColors().toCardColors()

    /**
     * Creates a [CardColors] that represents the default container and content colors used in a
     * [Card].
     *
     * @param containerColor the container color of this [Card] when enabled.
     * @param contentColor the content color of this [Card] when enabled.
     * @param disabledContainerColor the container color of this [Card] when not enabled.
     * @param disabledContentColor the content color of this [Card] when not enabled.
     */
    @Composable
    fun cardColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = contentColorFor(backgroundColor = containerColor),
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = contentColor.copy(alpha = DISABLED_ALPHA),
    ): CardColors = Material3CardDefaults.cardColors(
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = disabledContentColor,
    ).toCardColors()

    /**
     * Creates a [CardColors] that represents the default container and content colors used in an
     * [ElevatedCard].
     */
    @Composable
    fun elevatedCardColors(): CardColors = Material3CardDefaults.elevatedCardColors().toCardColors()

    /**
     * Creates a [CardColors] that represents the default container and content colors used in an
     * [ElevatedCard].
     *
     * @param containerColor the container color of this [ElevatedCard] when enabled.
     * @param contentColor the content color of this [ElevatedCard] when enabled.
     * @param disabledContainerColor the container color of this [ElevatedCard] when not enabled.
     * @param disabledContentColor the content color of this [ElevatedCard] when not enabled.
     */
    @Composable
    fun elevatedCardColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = contentColorFor(backgroundColor = containerColor),
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = contentColor.copy(alpha = DISABLED_ALPHA),
    ): CardColors = Material3CardDefaults.elevatedCardColors(
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = disabledContentColor,
    ).toCardColors()

    /**
     * Creates a [CardColors] that represents the default container and content colors used in an
     * [OutlinedCard].
     */
    @Composable
    fun outlinedCardColors(): CardColors = Material3CardDefaults.outlinedCardColors().toCardColors()

    /**
     * Creates a [CardColors] that represents the default container and content colors used in an
     * [OutlinedCard].
     *
     * @param containerColor the container color of this [OutlinedCard] when enabled.
     * @param contentColor the content color of this [OutlinedCard] when enabled.
     * @param disabledContainerColor the container color of this [OutlinedCard] when not enabled.
     * @param disabledContentColor the content color of this [OutlinedCard] when not enabled.
     */
    @Composable
    fun outlinedCardColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = contentColorFor(backgroundColor = containerColor),
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = contentColorFor(backgroundColor = containerColor).copy(alpha = DISABLED_ALPHA),
    ): CardColors = Material3CardDefaults.outlinedCardColors(
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = disabledContentColor,
    ).toCardColors()

    /**
     * Creates a [BorderStroke] that represents the default border used in [OutlinedCard].
     *
     * @param enabled whether the card is enabled
     */
    @Composable
    fun outlinedCardBorder(enabled: Boolean = true): BorderStroke =
        Material3CardDefaults.outlinedCardBorder(enabled)
}
