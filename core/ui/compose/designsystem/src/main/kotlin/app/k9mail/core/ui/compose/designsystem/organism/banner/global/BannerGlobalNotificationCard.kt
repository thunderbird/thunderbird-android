package app.k9mail.core.ui.compose.designsystem.organism.banner.global

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import app.k9mail.core.ui.compose.designsystem.atom.card.CardColors
import app.k9mail.core.ui.compose.designsystem.atom.card.CardFilled
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleSmall
import app.k9mail.core.ui.compose.designsystem.organism.banner.BannerNotificationCardDefaults
import app.k9mail.core.ui.compose.theme2.LocalContentColor
import app.k9mail.core.ui.compose.theme2.MainTheme

/**
 * Used to maintain the user’s awareness of a persistent irregular state of the application,
 * without disrupting other elements of the flow.
 *
 * @param icon The icon to display on the left side of the card.
 * @param text The text to display in the card.
 * @param modifier The modifier to apply to this layout node.
 * @param colors The colors to use for the card.
 * @param shape The shape of the card.
 * @param action The action to display on the right side of the card.
 */
@Composable
internal fun BannerGlobalNotificationCard(
    icon: @Composable () -> Unit,
    text: CharSequence,
    modifier: Modifier = Modifier,
    colors: CardColors = BannerNotificationCardDefaults.warningCardColors(),
    shape: Shape = BannerNotificationCardDefaults.bannerGlobalShape,
    action: (@Composable () -> Unit)? = null,
) {
    BannerGlobalNotificationCard(
        icon = icon,
        text = {
            when (text) {
                is String -> TextTitleSmall(
                    text = text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                is AnnotatedString -> TextTitleSmall(
                    text = text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                else -> TextTitleSmall(
                    text = text.toString(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        colors = colors,
        shape = shape,
        modifier = modifier,
        action = action,
    )
}

/**
 * Displays a header notification card.
 *
 * @param icon The icon to display on the left side of the card.
 * @param text The text to display in the card.
 * @param modifier The modifier to apply to this layout node.
 * @param colors The colors to use for the card.
 * @param shape The shape of the card.
 * @param action The action to display on the right side of the card.
 *
 * @see BannerGlobalNotificationCard
 */
@Composable
private fun BannerGlobalNotificationCard(
    icon: @Composable () -> Unit,
    text: @Composable () -> Unit,
    colors: CardColors,
    shape: Shape,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    CardFilled(
        modifier = modifier,
        shape = shape,
        colors = colors,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MainTheme.sizes.bannerGlobalHeight)
                .padding(horizontal = MainTheme.spacings.default),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
        ) {
            icon()
            Box(
                modifier = Modifier.weight(1f),
            ) {
                text()
            }
            CompositionLocalProvider(LocalContentColor provides colors.contentColor) {
                action?.invoke()
            }
        }
    }
}
