package net.thunderbird.feature.mail.message.list.ui.component.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import net.thunderbird.components.ui.bolt.atom.text.TextTitleSmall
import net.thunderbird.components.ui.bolt.common.window.WindowWidthSizeClass
import net.thunderbird.components.ui.bolt.common.window.calculateWindowSizeInfo
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.feature.mail.message.list.ui.component.config.MessageItemAccountIndicator
import net.thunderbird.feature.mail.message.list.ui.component.config.MessageItemConfiguration

/**
 * Renders an adaptive header row for a message item that adjusts its layout based
 * on screen size.
 *
 * The function determines the appropriate layout by checking the current window
 * size class and renders either a [HeaderRowSmall] or [HeaderRow] accordingly.
 *
 * @param configuration The message item configuration containing display settings,
 *  including the account indicator to be shown in the header.
 * @param receivedAt The timestamp string indicating when the message was received,
 *  displayed at the end of the header row.
 * @param firstLine A composable lambda that provides the custom content to be displayed
 *  as the first line of the header, typically containing sender information.
 * @param modifier Optional modifier to be applied to the header row container.
 */
@Composable
internal fun AdaptiveMessageItemHeaderRow(
    configuration: MessageItemConfiguration,
    receivedAt: String,
    firstLine: @Composable (() -> Unit),
    modifier: Modifier = Modifier,
) {
    val windowSizeInfo = calculateWindowSizeInfo()
    val isSmallScreen = windowSizeInfo.sizeClass.widthSizeClass == WindowWidthSizeClass.Small

    if (isSmallScreen) {
        HeaderRowSmall(modifier) {
            HeaderRowContent(isSmallScreen = true, configuration.accountIndicator, receivedAt, firstLine)
        }
    } else {
        HeaderRow(modifier) {
            HeaderRowContent(isSmallScreen = false, configuration.accountIndicator, receivedAt, firstLine)
        }
    }
}

@Composable
private fun HeaderRow(
    modifier: Modifier = Modifier,
    headerRowContent: @Composable RowScope.() -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(BoltTheme.spacings.default),
        modifier = modifier
            .defaultMinSize(minHeight = AccountIndicatorIcon.ACCOUNT_INDICATOR_DEFAULT_HEIGHT)
            .fillMaxWidth(),
        content = headerRowContent,
    )
}

@Composable
private fun HeaderRowSmall(
    modifier: Modifier = Modifier,
    headerRowContent: @Composable RowScope.() -> Unit,
) {
    FlowRow(
        verticalArrangement = Arrangement.spacedBy(BoltTheme.spacings.quarter),
        horizontalArrangement = Arrangement.Start,
        maxLines = 2,
        modifier = modifier.defaultMinSize(minHeight = AccountIndicatorIcon.ACCOUNT_INDICATOR_DEFAULT_HEIGHT),
    ) {
        headerRowContent()
    }
}

@Composable
private fun RowScope.HeaderRowContent(
    isSmallScreen: Boolean,
    accountIndicator: MessageItemAccountIndicator?,
    receivedAt: String,
    firstLine: @Composable (() -> Unit),
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .then(if (isSmallScreen) Modifier.fillMaxWidth() else Modifier.weight(1f))
            .defaultMinSize(minHeight = AccountIndicatorIcon.ACCOUNT_INDICATOR_DEFAULT_HEIGHT),
    ) {
        val indicatorColor = accountIndicator?.color
        if (indicatorColor != null) {
            AccountIndicatorIcon(indicatorColor)
        }
        firstLine()
    }
    TextTitleSmall(
        text = receivedAt,
        maxLines = 1,
        overflow = TextOverflow.Visible,
        modifier = Modifier.align(Alignment.CenterVertically),
    )
}
