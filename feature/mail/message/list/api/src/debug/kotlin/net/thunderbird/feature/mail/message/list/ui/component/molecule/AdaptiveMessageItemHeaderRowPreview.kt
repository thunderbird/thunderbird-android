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
import app.k9mail.core.ui.compose.common.window.WindowSizeClass
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemeLightDark
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleSmall
import net.thunderbird.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.mail.message.list.ui.component.config.MessageItemAccountIndicator
import net.thunderbird.feature.mail.message.list.ui.component.config.MessageItemConfiguration

private class AdaptiveMessageItemHeaderRowPreviewData(
    val previewName: String,
    val configuration: MessageItemConfiguration,
    val receivedAt: String,
    val screenWidth: Int,
)

private val screenWidths = listOf(
    "Small" to (WindowSizeClass.SMALL_MAX_WIDTH - 1),
    "Compact" to (WindowSizeClass.COMPACT_MAX_WIDTH - 1),
    "Medium" to (WindowSizeClass.MEDIUM_MAX_WIDTH - 1),
    "Expanded" to WindowSizeClass.MEDIUM_MAX_WIDTH,
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
                screenWidthDp = data.screenWidth
            },
        ) {
            AdaptiveMessageItemHeaderRow(
                configuration = data.configuration,
                receivedAt = data.receivedAt,
                firstLine = { TextTitleSmall(data.previewName) },
                modifier = Modifier.padding(
                    horizontal = MainTheme.spacings.default,
                    vertical = MainTheme.spacings.quadruple,
                ),
            )
        }
    }
}
