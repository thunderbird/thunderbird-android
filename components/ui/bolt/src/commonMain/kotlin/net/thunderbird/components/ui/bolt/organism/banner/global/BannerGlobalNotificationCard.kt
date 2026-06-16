package net.thunderbird.components.ui.bolt.organism.banner.global

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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import net.thunderbird.components.ui.bolt.PreviewWithThemesLightDark
import net.thunderbird.components.ui.bolt.atom.Surface
import net.thunderbird.components.ui.bolt.atom.button.ButtonText
import net.thunderbird.components.ui.bolt.atom.card.CardColors
import net.thunderbird.components.ui.bolt.atom.card.CardFilled
import net.thunderbird.components.ui.bolt.atom.icon.Icon
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.atom.text.TextTitleSmall
import net.thunderbird.components.ui.bolt.organism.banner.BannerNotificationCardDefaults
import net.thunderbird.components.ui.bolt.theme.LocalContentColor
import net.thunderbird.components.ui.bolt.theme.BoltTheme

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
                .heightIn(min = BoltTheme.sizes.bannerGlobalHeight)
                .padding(horizontal = BoltTheme.spacings.default),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BoltTheme.spacings.default),
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

@PreviewLightDark
@Composable
private fun BannerGlobalNotificationCardStringTitlePreview() {
    PreviewWithThemesLightDark {
        Surface(
            modifier = Modifier.fillMaxWidth(),
        ) {
            BannerGlobalNotificationCard(
                icon = { Icon(imageVector = Icons.Outlined.Warning) },
                text = "Offline. No internet connection found.",
                action = {
                    ButtonText(
                        text = "Retry",
                        onClick = {},
                    )
                },
                modifier = Modifier.padding(top = BoltTheme.spacings.quadruple),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun BannerGlobalNotificationCardAnnotatedStringTitlePreview() {
    PreviewWithThemesLightDark {
        Surface(
            modifier = Modifier.fillMaxWidth(),
        ) {
            BannerGlobalNotificationCard(
                icon = { Icon(imageVector = Icons.Outlined.Warning) },
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Black)) {
                        append("Offline. ")
                    }
                    append("No internet connection found.")
                },
                action = {
                    ButtonText(
                        text = "Retry",
                        onClick = {},
                    )
                },
                modifier = Modifier.padding(top = BoltTheme.spacings.quadruple),
            )
        }
    }
}
