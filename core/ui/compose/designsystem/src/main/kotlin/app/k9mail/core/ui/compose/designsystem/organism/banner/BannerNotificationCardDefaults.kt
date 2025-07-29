package app.k9mail.core.ui.compose.designsystem.organism.banner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.card.CardColors
import app.k9mail.core.ui.compose.designsystem.atom.card.CardDefaults
import app.k9mail.core.ui.compose.designsystem.organism.banner.global.BannerGlobalNotificationCard
import app.k9mail.core.ui.compose.designsystem.organism.banner.inline.BannerInlineNotificationCard
import app.k9mail.core.ui.compose.designsystem.organism.banner.inline.BannerInlineNotificationCardBehaviour
import app.k9mail.core.ui.compose.theme2.MainTheme

/**
 * Contains the default values used by [BannerInlineNotificationCard] and [BannerGlobalNotificationCard] types
 */
object BannerNotificationCardDefaults {
    /** The default shape of the [BannerGlobalNotificationCard] */
    val bannerGlobalShape: Shape = RectangleShape

    /** The default shape of the [BannerInlineNotificationCard] */
    val bannerInlineShape: Shape
        @ReadOnlyComposable
        @Composable
        get() = RoundedCornerShape(size = 12.dp)

    /**
     * The default behaviour for the [BannerInlineNotificationCard]
     */
    val behaviour = BannerInlineNotificationCardBehaviour.Expanded

    /**
     * Creates a [CardColors] for an error banner inline notification card.
     *
     * @param containerColor The color used for the background of this card.
     * @param contentColor The preferred color for content inside this card.
     *
     * @return A [CardColors] with the specified colors.
     */
    @Composable
    fun errorCardColors(
        containerColor: Color = MainTheme.colors.errorContainer,
        contentColor: Color = MainTheme.colors.onErrorContainer,
    ): CardColors = CardDefaults.outlinedCardColors(
        containerColor = containerColor,
        contentColor = contentColor,
    )

    /**
     * Creates a [CardColors] for an information banner inline notification card.
     *
     * @param containerColor The container color of the card.
     * @param contentColor The content color of the card.
     *
     * @return A [CardColors] with the specified colors.
     */
    @Composable
    fun infoCardColors(
        containerColor: Color = MainTheme.colors.infoContainer,
        contentColor: Color = MainTheme.colors.onInfoContainer,
    ): CardColors = CardDefaults.outlinedCardColors(
        containerColor = containerColor,
        contentColor = contentColor,
    )

    /**
     * Creates a [CardColors] for a warning banner inline notification card.
     *
     * @param containerColor The container color of the card.
     * @param contentColor The content color of the card.
     *
     * @return A [CardColors] with the specified colors.
     */
    @Composable
    fun warningCardColors(
        containerColor: Color = MainTheme.colors.warningContainer,
        contentColor: Color = MainTheme.colors.onWarningContainer,
    ): CardColors = CardDefaults.outlinedCardColors(
        containerColor = containerColor,
        contentColor = contentColor,
    )

    /**
     * Creates a [CardColors] for a success banner inline notification card.
     *
     * @param containerColor The container color of the card.
     * @param contentColor The content color of the card.
     *
     * @return A [CardColors] with the specified colors.
     */
    @Composable
    fun successCardColors(
        containerColor: Color = MainTheme.colors.successContainer,
        contentColor: Color = MainTheme.colors.onSuccessContainer,
    ): CardColors = CardDefaults.outlinedCardColors(
        containerColor = containerColor,
        contentColor = contentColor,
    )

    /**
     * Creates a [BorderStroke] for the error banner inline notification card.
     *
     * @return The [BorderStroke] for the error banner inline notification card.
     */
    @Composable
    fun errorCardBorder(): BorderStroke = defaultCardBorder(color = MainTheme.colors.onErrorContainer)

    /**
     * Creates a [BorderStroke] for the info banner inline notification card.
     *
     * @return The [BorderStroke] for the info banner inline notification card.
     */
    @Composable
    fun infoCardBorder(): BorderStroke = defaultCardBorder(color = MainTheme.colors.onInfoContainer)

    /**
     * Creates a [BorderStroke] for the warning banner inline notification card.
     *
     * @return The [BorderStroke] for the warning banner inline notification card.
     */
    @Composable
    fun warningCardBorder(): BorderStroke = defaultCardBorder(color = MainTheme.colors.onWarningContainer)

    /**
     * Creates a [BorderStroke] for the success banner inline notification card.
     *
     * @return The [BorderStroke] for the success banner inline notification card.
     */
    @Composable
    fun successCardBorder(): BorderStroke = defaultCardBorder(color = MainTheme.colors.onSuccessContainer)

    private fun defaultCardBorder(color: Color): BorderStroke = BorderStroke(
        width = 1.dp,
        brush = SolidColor(color),
    )
}
