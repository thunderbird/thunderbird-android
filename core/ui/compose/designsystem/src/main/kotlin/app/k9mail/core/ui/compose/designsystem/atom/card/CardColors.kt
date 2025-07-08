package app.k9mail.core.ui.compose.designsystem.atom.card

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.CardColors as Material3CardColors

/**
 * Represents the colors used by a card.
 *
 * See [CardDefaults.cardColors], [CardDefaults.outlinedCardColors], and [CardDefaults.elevatedCardColors].
 *
 * @property containerColor The color used for the background of this card.
 * @property contentColor The preferred color for content inside this card.
 * @property disabledContainerColor The color used for the background of this card when it is not enabled.
 * @property disabledContentColor The preferred color for content inside this card when it is not enabled.
 */
data class CardColors(
    val containerColor: Color,
    val contentColor: Color,
    val disabledContainerColor: Color,
    val disabledContentColor: Color,
)

/**
 * Converts a [Material3CardColors] to a [CardColors].
 */
internal fun Material3CardColors.toCardColors(): CardColors =
    CardColors(
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = disabledContentColor,
    )

/**
 * Converts a [CardColors] to a Material 3 [Material3CardColors].
 */
internal fun CardColors.toMaterial3CardColors(): Material3CardColors =
    Material3CardColors(
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = disabledContentColor,
    )
