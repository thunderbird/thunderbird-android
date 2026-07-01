package net.thunderbird.components.ui.bolt.organism.banner.inline

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import net.thunderbird.components.ui.bolt.PreviewWithThemesLightDark
import net.thunderbird.components.ui.bolt.atom.Surface
import net.thunderbird.components.ui.bolt.atom.card.CardColors
import net.thunderbird.components.ui.bolt.atom.card.CardOutlined
import net.thunderbird.components.ui.bolt.atom.icon.Icon
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.atom.text.TextTitleMedium
import net.thunderbird.components.ui.bolt.atom.text.TextTitleSmall
import net.thunderbird.components.ui.bolt.molecule.notification.NotificationActionButton
import net.thunderbird.components.ui.bolt.organism.banner.BannerNotificationCardDefaults
import net.thunderbird.components.ui.bolt.organism.banner.BannerNotificationCardDefaults.TEST_TAG_BANNER_INLINE_CARD_ACTION_ROW
import net.thunderbird.components.ui.bolt.theme.LocalContentColor
import net.thunderbird.components.ui.bolt.theme.BoltTheme

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
    behaviour: BannerInlineNotificationCardBehaviour = BannerNotificationCardDefaults.bannerInlineBehaviour,
    onSupportingTextOverflow: (hasVisualOverflow: Boolean) -> Unit = {},
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
                onTextOverflow = { hasVisualOverflow ->
                    if (behaviour == BannerInlineNotificationCardBehaviour.Clipped) {
                        onSupportingTextOverflow(hasVisualOverflow)
                    }
                },
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
            modifier = Modifier.padding(BoltTheme.spacings.oneHalf),
            verticalArrangement = Arrangement.spacedBy(BoltTheme.spacings.default),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(BoltTheme.spacings.default),
            ) {
                CompositionLocalProvider(LocalContentColor provides colors.contentColor) {
                    icon()
                    Column(verticalArrangement = Arrangement.spacedBy(BoltTheme.spacings.quarter)) {
                        title()
                        supportingText()
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(
                    space = BoltTheme.spacings.default,
                    alignment = Alignment.End,
                ),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TEST_TAG_BANNER_INLINE_CARD_ACTION_ROW),
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
private fun BannerInlineNotificationSupportingText(
    supportingText: CharSequence,
    behaviour: BannerInlineNotificationCardBehaviour,
    maxLines: Int,
    modifier: Modifier = Modifier,
    onTextOverflow: (hasVisualOverflow: Boolean) -> Unit = {},
) {
    val clippedSupportingText = remember(supportingText, behaviour) {
        when (behaviour) {
            BannerInlineNotificationCardBehaviour.Clipped if supportingText.length > MAX_SUPPORTING_TEXT_LENGTH ->
                supportingText.subSequence(startIndex = 0, endIndex = MAX_SUPPORTING_TEXT_LENGTH)

            else -> supportingText
        }
    }
    val onTextLayout = remember<(TextLayoutResult) -> Unit>(onTextOverflow) {
        { textLayoutResult -> onTextOverflow(textLayoutResult.hasVisualOverflow) }
    }
    when (clippedSupportingText) {
        is String -> TextBodyMedium(
            text = clippedSupportingText,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
            onTextLayout = onTextLayout,
        )

        is AnnotatedString -> TextBodyMedium(
            text = clippedSupportingText,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
            onTextLayout = onTextLayout,
        )

        else -> TextBodyMedium(
            text = clippedSupportingText.toString(),
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
            onTextLayout = onTextLayout,
        )
    }
}

@PreviewLightDark
@Composable
private fun BannerInlineNotificationCardCustomTitleAndDescriptionPreview() {
    PreviewWithThemesLightDark {
        Surface(modifier = Modifier.padding(BoltTheme.spacings.triple)) {
            BannerInlineNotificationCard(
                icon = { Icon(imageVector = Icons.Outlined.Report) },
                title = {
                    TextTitleMedium(text = "Authentication required")
                },
                supportingText = {
                    TextBodyMedium(text = "Sign in to authenticate username@domain3.example")
                },
                actions = {
                    NotificationActionButton(text = "Support article", onClick = {}, isExternalLink = true)
                    NotificationActionButton(text = "Sign in", onClick = {})
                },
                modifier = Modifier.padding(
                    vertical = BoltTheme.spacings.quadruple,
                    horizontal = BoltTheme.spacings.default,
                ),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun BannerInlineNotificationCardTextPreview() {
    PreviewWithThemesLightDark {
        Surface(modifier = Modifier.padding(BoltTheme.spacings.triple)) {
            BannerInlineNotificationCard(
                icon = { Icon(imageVector = Icons.Outlined.Report) },
                title = "Missing encryption key",
                supportingText = "To dismiss this error, disable encryption for this account or ensure " +
                    "encryption key is available in openKeychain app.",
                actions = {
                    NotificationActionButton(text = "Support article", onClick = {}, isExternalLink = true)
                    NotificationActionButton(text = "Disable encryption", onClick = {})
                },
                modifier = Modifier.padding(
                    vertical = BoltTheme.spacings.quadruple,
                    horizontal = BoltTheme.spacings.default,
                ),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun BannerInlineNotificationCardAnnotatedStringPreview() {
    PreviewWithThemesLightDark {
        Surface(modifier = Modifier.padding(BoltTheme.spacings.triple)) {
            BannerInlineNotificationCard(
                icon = { Icon(imageVector = Icons.Outlined.Report) },
                title = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = BoltTheme.colors.tertiaryContainer)) {
                        append("Missing encryption key")
                    }
                },
                supportingText = buildAnnotatedString {
                    append("To dismiss this error, ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Black)) {
                        append("disable encryption for this account or ensure encryption key is available")
                    }
                    append("in openKeychain app.")
                },
                actions = {
                    NotificationActionButton(text = "Support article", onClick = {}, isExternalLink = true)
                    NotificationActionButton(text = "Disable encryption", onClick = {})
                },
                modifier = Modifier.padding(
                    vertical = BoltTheme.spacings.quadruple,
                    horizontal = BoltTheme.spacings.default,
                ),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun BannerInlineNotificationClippedCardTextPreview() {
    PreviewWithThemesLightDark {
        Surface(modifier = Modifier.padding(BoltTheme.spacings.triple)) {
            BannerInlineNotificationCard(
                icon = { Icon(imageVector = Icons.Outlined.Report) },
                title = "Vestibulum tempor sed massa eget fermentum. Vivamus ut vitae aliquam e augue. " +
                    "Sed nec tincidunt arcu",
                supportingText = "scelerisque fermentum. In lobortis pellentesque aliquet. Curabitur quam " +
                    "felis, sodales in leo ac, sodales rutrum quam. Quisque et odio id ex varius porta. " +
                    "Vestibulum tortor nibh, porta venenatis velit",
                actions = {
                    NotificationActionButton(text = "Support article", onClick = {}, isExternalLink = true)
                    NotificationActionButton(text = "Disable encryption", onClick = {})
                },
                modifier = Modifier.padding(
                    vertical = BoltTheme.spacings.quadruple,
                    horizontal = BoltTheme.spacings.default,
                ),
                behaviour = BannerInlineNotificationCardBehaviour.Clipped,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun BannerInlineNotificationExpandedCardTextPreview() {
    PreviewWithThemesLightDark {
        Surface(modifier = Modifier.padding(BoltTheme.spacings.triple)) {
            BannerInlineNotificationCard(
                icon = { Icon(imageVector = Icons.Outlined.Report) },
                title = "Vestibulum tempor sed massa eget fermentum. Vivamus ut vitae aliquam e augue. " +
                    "Sed nec tincidunt arcu",
                supportingText = "scelerisque fermentum. In lobortis pellentesque aliquet. Curabitur quam " +
                    "felis, sodales in leo ac, sodales rutrum quam. Quisque et odio id ex varius porta. " +
                    "Vestibulum tortor nibh, porta venenatis velit",
                actions = {
                    NotificationActionButton(text = "Support article", onClick = {}, isExternalLink = true)
                    NotificationActionButton(text = "Disable encryption", onClick = {})
                },
                modifier = Modifier.padding(
                    vertical = BoltTheme.spacings.quadruple,
                    horizontal = BoltTheme.spacings.default,
                ),
                behaviour = BannerInlineNotificationCardBehaviour.Expanded,
            )
        }
    }
}
