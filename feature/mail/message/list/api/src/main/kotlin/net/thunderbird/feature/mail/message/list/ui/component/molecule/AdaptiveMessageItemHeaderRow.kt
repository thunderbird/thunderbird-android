package net.thunderbird.feature.mail.message.list.ui.component.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import app.k9mail.core.ui.compose.common.window.WindowSizeClass
import app.k9mail.core.ui.compose.common.window.getWindowSizeInfo
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleSmall
import net.thunderbird.core.ui.compose.theme2.MainTheme
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
    val windowSizeInfo = getWindowSizeInfo()
    val isSmallScreen = windowSizeInfo.screenWidthSizeClass == WindowSizeClass.Small

    val headerRowContent: @Composable ((RowScope) -> Unit) = rememberAdaptiveHeaderRowContent(
        isSmallScreen = isSmallScreen,
        accountIndicator = configuration.accountIndicator,
        receivedAt = receivedAt,
        firstLine = firstLine,
    )
    if (isSmallScreen) {
        HeaderRowSmall(modifier, headerRowContent = headerRowContent)
    } else {
        HeaderRow(modifier, headerRowContent = headerRowContent)
    }
}

@Composable
private fun HeaderRow(
    modifier: Modifier = Modifier,
    headerRowContent: @Composable ((RowScope) -> Unit),
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
        modifier = modifier
            .defaultMinSize(minHeight = AccountIndicatorIcon.ACCOUNT_INDICATOR_DEFAULT_HEIGHT)
            .fillMaxWidth()
            .width(intrinsicSize = IntrinsicSize.Max),
    ) {
        headerRowContent(this)
    }
}

@Composable
private fun HeaderRowSmall(
    modifier: Modifier = Modifier,
    headerRowContent: @Composable ((RowScope) -> Unit),
) {
    FlowRow(
        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.quarter),
        horizontalArrangement = Arrangement.Start,
        maxLines = 2,
        modifier = modifier.defaultMinSize(minHeight = AccountIndicatorIcon.ACCOUNT_INDICATOR_DEFAULT_HEIGHT),
    ) {
        headerRowContent(this)
    }
}

@Composable
private fun rememberAdaptiveHeaderRowContent(
    isSmallScreen: Boolean,
    accountIndicator: MessageItemAccountIndicator?,
    receivedAt: String,
    firstLine: @Composable (() -> Unit),
): @Composable ((RowScope) -> Unit) = remember(isSmallScreen, accountIndicator, receivedAt, firstLine) {
    movableContentOf { scope ->
        with(scope) {
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
    }
}
