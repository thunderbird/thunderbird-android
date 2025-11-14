package net.thunderbird.core.ui.compose.designsystem.atom.pager

import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemeLightDark

@PreviewLightDark
@Composable
private fun TabPreviewParam(
    @PreviewParameter(TabPreviewParamCol::class) param: TabPreviewParam,
) {
    PreviewWithThemeLightDark {
        Surface {
            Tab(
                selected = param.active,
                title = { Text(text = param.title) },
                onClick = { },
                enabled = param.enabled,
            )
        }
    }
}

private class TabPreviewParamCol : CollectionPreviewParameterProvider<TabPreviewParam>(
    setOf(
        TabPreviewParam(
            active = true,
            title = "Active Tab",
            enabled = true,
        ),
        TabPreviewParam(
            active = false,
            title = "Unactive Tab",
            enabled = true,
        ),
        TabPreviewParam(
            active = true,
            title = "Disabled Tab",
            enabled = false,
        ),
        TabPreviewParam(
            active = false,
            title = "Active Tab",
            enabled = false,
        ),
    ),
)

private data class TabPreviewParam(
    val active: Boolean,
    val title: String,
    val enabled: Boolean,
)
