package app.k9mail.core.ui.compose.designsystem.organism.banner.inline

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import app.k9mail.core.ui.compose.designsystem.atom.card.CardColors
import app.k9mail.core.ui.compose.designsystem.atom.card.CardOutlined
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleSmall
import app.k9mail.core.ui.compose.designsystem.organism.banner.BannerNotificationCardDefaults
import app.k9mail.core.ui.compose.theme2.LocalContentColor
import app.k9mail.core.ui.compose.theme2.MainTheme

private const val MAX_TITLE_LENGTH = 100
private const val MAX_SUPPORTING_TEXT_LENGTH = 200

/**
 * Used to inform the user that something needs their attention before interacting with
 * the main content on the screen.
 *
 * @param icon The icon to display on the left side of the notification card.
 * @param title The title of the notification card.
 * @param supportingText The supporting text of the notification card.
 * @param actions The actions to display at the bottom of the notification card.
 * @param modifier The modifier to apply to the notification card.
 * @param colors The colors to use for the notification card.
 * @param border The border to use for the notification card.
 * @param shape The shape to use for the notification card.
 * @param behaviour The behaviour to use for the notification card.
 * @see BannerNotificationCardDefaults
 */
@Composable
internal fun BannerInlineNotificationCard(
    icon: @Composable () -> Unit,
    title: CharSequence,
    supportingText: CharSequence,
    actions: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    colors: CardColors = BannerNotificationCardDefaults.errorCardColors(),
    border: BorderStroke = BannerNotificationCardDefaults.errorCardBorder(),
    shape: Shape = BannerNotificationCardDefaults.bannerInlineShape,
    behaviour: BannerInlineNotificationCardBehaviour = BannerNotificationCardDefaults.behaviour,
) {
    val maxLines = when (behaviour) {
        BannerInlineNotificationCardBehaviour.Clipped -> 2
        BannerInlineNotificationCardBehaviour.Expanded -> Int.MAX_VALUE
    }

    BannerInlineNotificationCard(
        icon = icon,
        title = {
            BannerInlineNotificationTitle(
                title = title,
                behaviour = behaviour,
                maxLines = maxLines,
            )
        },
        supportingText = {
            BannerInlineNotificationSupportingText(
                supportingText = supportingText,
                behaviour = behaviour,
                maxLines = maxLines,
            )
        },
        actions = actions,
        modifier = modifier,
        colors = colors,
        border = border,
        shape = shape,
    )
}

/**
 * Displays an inline notification card.
 *
 * @param icon The icon to display on the left side of the notification card.
 * @param title The title of the notification card.
 * @param supportingText The supporting text of the notification card.
 * @param actions The actions to display at the bottom of the notification card.
 * @param modifier The modifier to apply to the notification card.
 *
 * @see BannerInlineNotificationCard
 */
@Composable
internal fun BannerInlineNotificationCard(
    icon: @Composable () -> Unit,
    title: @Composable () -> Unit,
    supportingText: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    colors: CardColors = BannerNotificationCardDefaults.errorCardColors(),
    border: BorderStroke = BannerNotificationCardDefaults.errorCardBorder(),
    shape: Shape = BannerNotificationCardDefaults.bannerInlineShape,
) {
    CardOutlined(
        modifier = modifier,
        colors = colors,
        border = border,
        shape = shape,
    ) {
        Column(
            modifier = Modifier.padding(MainTheme.spacings.oneHalf),
            verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
            ) {
                CompositionLocalProvider(LocalContentColor provides colors.contentColor) {
                    icon()
                    Column(verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.quarter)) {
                        title()
                        supportingText()
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(
                    space = MainTheme.spacings.default,
                    alignment = Alignment.End,
                ),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                CompositionLocalProvider(LocalContentColor provides colors.contentColor) {
                    actions()
                }
            }
        }
    }
}

@Composable
private fun BannerInlineNotificationTitle(
    title: CharSequence,
    behaviour: BannerInlineNotificationCardBehaviour,
    maxLines: Int,
    modifier: Modifier = Modifier,
) {
    val clippedTitle = remember(title, behaviour) {
        when (behaviour) {
            BannerInlineNotificationCardBehaviour.Clipped if title.length > MAX_TITLE_LENGTH ->
                title.subSequence(startIndex = 0, endIndex = MAX_TITLE_LENGTH)

            else -> title
        }
    }

    when (clippedTitle) {
        is String -> TextTitleSmall(
            text = clippedTitle,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
        )

        is AnnotatedString -> TextTitleSmall(
            text = clippedTitle,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
        )

        else -> TextTitleSmall(
            text = clippedTitle.toString(),
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
        )
    }
}

@Composable
fun BannerInlineNotificationSupportingText(
    supportingText: CharSequence,
    behaviour: BannerInlineNotificationCardBehaviour,
    maxLines: Int,
    modifier: Modifier = Modifier,
) {
    val clippedSupportingText = remember(supportingText, behaviour) {
        when (behaviour) {
            BannerInlineNotificationCardBehaviour.Clipped if supportingText.length > MAX_SUPPORTING_TEXT_LENGTH ->
                supportingText.subSequence(startIndex = 0, endIndex = MAX_SUPPORTING_TEXT_LENGTH)

            else -> supportingText
        }
    }
    when (clippedSupportingText) {
        is String -> TextBodyMedium(
            text = clippedSupportingText,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
        )

        is AnnotatedString -> TextBodyMedium(
            text = clippedSupportingText,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
        )

        else -> TextBodyMedium(
            text = clippedSupportingText.toString(),
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
        )
    }
}
