package app.k9mail.core.ui.compose.designsystem.atom.card

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.CardDefaults as Material3CardDefaults
import androidx.compose.material3.CardElevation as Material3CardElevation

/**
 * Represents the elevation for a card in different states.
 *
 * This sealed interface defines the elevation properties for various interaction states of a card component.
 * It also provides a composable function to convert these elevations into a [Material3CardElevation] object.
 *
 * Different card types (Filled, Elevated, Outlined) will have their own specific implementations of this interface.
 *
 * - See [CardDefaults.cardElevation] for the default elevation used in a [CardFilled].
 * - See [CardDefaults.elevatedCardElevation] for the default elevation used in an [CardElevated].
 * - See [CardDefaults.outlinedCardElevation] for the default elevation used in an [CardOutlined].
 *
 * @property defaultElevation The elevation used by default.
 * @property pressedElevation The elevation used when the card is pressed.
 * @property focusedElevation The elevation used when the card is focused.
 * @property hoveredElevation The elevation used when the card is hovered.
 * @property draggedElevation The elevation used when the card is dragged.
 * @property disabledElevation The elevation used when the card is disabled.
 */
sealed interface CardElevation {
    val defaultElevation: Dp
    val pressedElevation: Dp
    val focusedElevation: Dp
    val hoveredElevation: Dp
    val draggedElevation: Dp
    val disabledElevation: Dp

    /**
     * Converts this [CardElevation] to a Material 3 [Material3CardElevation].
     */
    @Composable
    fun toMaterial3CardElevation(): Material3CardElevation

    @ConsistentCopyVisibility
    data class FilledCardElevation internal constructor(
        override val defaultElevation: Dp,
        override val pressedElevation: Dp,
        override val focusedElevation: Dp,
        override val hoveredElevation: Dp,
        override val draggedElevation: Dp,
        override val disabledElevation: Dp,
    ) : CardElevation {
        @Composable
        override fun toMaterial3CardElevation(): Material3CardElevation =
            Material3CardDefaults.cardElevation(
                defaultElevation = defaultElevation,
                pressedElevation = pressedElevation,
                focusedElevation = focusedElevation,
                hoveredElevation = hoveredElevation,
                draggedElevation = draggedElevation,
                disabledElevation = disabledElevation,
            )
    }

    @ConsistentCopyVisibility
    data class ElevatedCardElevation internal constructor(
        override val defaultElevation: Dp,
        override val pressedElevation: Dp,
        override val focusedElevation: Dp,
        override val hoveredElevation: Dp,
        override val draggedElevation: Dp,
        override val disabledElevation: Dp,
    ) : CardElevation {
        @Composable
        override fun toMaterial3CardElevation(): Material3CardElevation =
            Material3CardDefaults.elevatedCardElevation(
                defaultElevation = defaultElevation,
                pressedElevation = pressedElevation,
                focusedElevation = focusedElevation,
                hoveredElevation = hoveredElevation,
                draggedElevation = draggedElevation,
                disabledElevation = disabledElevation,
            )
    }

    @ConsistentCopyVisibility
    data class OutlinedCardElevation internal constructor(
        override val defaultElevation: Dp,
        override val pressedElevation: Dp,
        override val focusedElevation: Dp,
        override val hoveredElevation: Dp,
        override val draggedElevation: Dp,
        override val disabledElevation: Dp,
    ) : CardElevation {
        @Composable
        override fun toMaterial3CardElevation(): Material3CardElevation =
            Material3CardDefaults.outlinedCardElevation(
                defaultElevation = defaultElevation,
                pressedElevation = pressedElevation,
                focusedElevation = focusedElevation,
                hoveredElevation = hoveredElevation,
                draggedElevation = draggedElevation,
                disabledElevation = disabledElevation,
            )
    }
}
