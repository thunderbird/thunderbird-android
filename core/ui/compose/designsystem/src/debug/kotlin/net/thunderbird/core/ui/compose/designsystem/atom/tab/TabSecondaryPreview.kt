package net.thunderbird.core.ui.compose.designsystem.atom.tab

import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemeLightDark

@PreviewLightDark
@Composable
private fun TabSecondaryPreview(
    @PreviewParameter(TabSecondaryPreviewParamCol::class) param: TabSecondaryPreviewParam,
) {
    PreviewWithThemeLightDark {
        Surface {
            TabSecondary(
                selected = param.active,
                title = { Text(text = param.title) },
                onClick = { },
                enabled = param.enabled,
            )
        }
    }
}

private class TabSecondaryPreviewParamCol : CollectionPreviewParameterProvider<TabSecondaryPreviewParam>(
    setOf(
        TabSecondaryPreviewParam(
            active = true,
            title = "Active TabSecondary",
            enabled = true,
        ),
        TabSecondaryPreviewParam(
            active = false,
            title = "Inactive TabSecondary",
            enabled = true,
        ),
        TabSecondaryPreviewParam(
            active = true,
            title = "Disabled TabSecondary",
            enabled = false,
        ),
        TabSecondaryPreviewParam(
            active = false,
            title = "Active TabSecondary",
            enabled = false,
        ),
    ),
)

private data class TabSecondaryPreviewParam(
    val active: Boolean,
    val title: String,
    val enabled: Boolean,
)
