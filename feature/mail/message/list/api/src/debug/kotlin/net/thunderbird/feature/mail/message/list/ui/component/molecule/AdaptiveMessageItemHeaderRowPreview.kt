package net.thunderbird.feature.mail.message.list.ui.component.molecule

import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.thunderbird.components.ui.bolt.PreviewWithThemeLightDark
import net.thunderbird.components.ui.bolt.atom.text.TextTitleSmall
import net.thunderbird.components.ui.bolt.common.window.WindowWidthSizeClass
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.feature.mail.message.list.ui.component.config.MessageItemAccountIndicator
import net.thunderbird.feature.mail.message.list.ui.component.config.MessageItemConfiguration

private class AdaptiveMessageItemHeaderRowPreviewData(
    val previewName: String,
    val configuration: MessageItemConfiguration,
    val receivedAt: String,
    val screenWidth: Dp,
)

private val screenWidths = listOf(
    "Small" to (WindowWidthSizeClass.BREAKPOINT_SMALL - 1.dp),
    "Compact" to (WindowWidthSizeClass.BREAKPOINT_COMPACT - 1.dp),
    "Medium" to (WindowWidthSizeClass.BREAKPOINT_MEDIUM - 1.dp),
    "Expanded" to WindowWidthSizeClass.BREAKPOINT_MEDIUM, // FIXME
)

private class AdaptiveMessageItemHeaderRowProvider :
    CollectionPreviewParameterProvider<AdaptiveMessageItemHeaderRowPreviewData>(
        screenWidths.flatMap { (sizeName, width) ->
            listOf(
                AdaptiveMessageItemHeaderRowPreviewData(
                    previewName = "$sizeName - Default",
                    configuration = MessageItemConfiguration(),
                    receivedAt = "10:30 AM",
                    screenWidth = width,
                ),
                AdaptiveMessageItemHeaderRowPreviewData(
                    previewName = "$sizeName - Yesterday timestamp",
                    configuration = MessageItemConfiguration(),
                    receivedAt = "Yesterday",
                    screenWidth = width,
                ),
                AdaptiveMessageItemHeaderRowPreviewData(
                    previewName = "$sizeName - Date timestamp",
                    configuration = MessageItemConfiguration(),
                    receivedAt = "Mar 12, 2026",
                    screenWidth = width,
                ),
                AdaptiveMessageItemHeaderRowPreviewData(
                    previewName = "$sizeName - With account indicator",
                    configuration = MessageItemConfiguration(
                        accountIndicator = MessageItemAccountIndicator(color = Color(color = 0xFF1565C0)),
                    ),
                    receivedAt = "9:15 AM",
                    screenWidth = width,
                ),
                AdaptiveMessageItemHeaderRowPreviewData(
                    previewName = "$sizeName - With red account indicator",
                    configuration = MessageItemConfiguration(
                        accountIndicator = MessageItemAccountIndicator(color = Color(color = 0xFFC62828)),
                    ),
                    receivedAt = "2:45 PM",
                    screenWidth = width,
                ),
                AdaptiveMessageItemHeaderRowPreviewData(
                    previewName = "$sizeName - With green account indicator",
                    configuration = MessageItemConfiguration(
                        accountIndicator = MessageItemAccountIndicator(color = Color(color = 0xFF2E7D32)),
                    ),
                    receivedAt = "Jan 5",
                    screenWidth = width,
                ),
            )
        },
    ) {
    override fun getDisplayName(index: Int): String = values.elementAt(index).previewName
}

@PreviewLightDark
@Composable
private fun AdaptiveMessageItemHeaderRowPreview(
    @PreviewParameter(AdaptiveMessageItemHeaderRowProvider::class) data: AdaptiveMessageItemHeaderRowPreviewData,
) {
    PreviewWithThemeLightDark {
        CompositionLocalProvider(
            LocalConfiguration provides Configuration().apply {
                screenWidthDp = data.screenWidth.value.toInt()
            },
        ) {
            AdaptiveMessageItemHeaderRow(
                configuration = data.configuration,
                receivedAt = data.receivedAt,
                firstLine = { TextTitleSmall(data.previewName) },
                modifier = Modifier.padding(
                    horizontal = BoltTheme.spacings.default,
                    vertical = BoltTheme.spacings.quadruple,
                ),
            )
        }
    }
}
