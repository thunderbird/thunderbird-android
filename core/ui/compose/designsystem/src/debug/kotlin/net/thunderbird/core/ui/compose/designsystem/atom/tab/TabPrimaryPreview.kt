package net.thunderbird.core.ui.compose.designsystem.atom.tab

import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemeLightDark
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelSmall
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons

@PreviewLightDark
@Composable
private fun TabPrimaryPreview(
    @PreviewParameter(TabPrimaryPreviewParamCol::class) param: TabPrimaryPreviewParam,
) {
    PreviewWithThemeLightDark {
        Surface {
            TabPrimary(
                selected = param.active,
                title = { Text(text = param.title) },
                onClick = { },
                enabled = param.enabled,
                icon = param.icon?.let {
                    { Icon(imageVector = param.icon) }
                },
                badge = param.badgeCount?.let {
                    {
                        TextLabelSmall(
                            text = param.badgeCount.toString(),
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                },
                badgeColor = MainTheme.colors.info,
            )
        }
    }
}

private class TabPrimaryPreviewParamCol : CollectionPreviewParameterProvider<TabPrimaryPreviewParam>(
    buildSet {
        addAll(createTabPrimaryPreviewParam(icon = null, badgeCount = null))
        addAll(createTabPrimaryPreviewParam(icon = Icons.Outlined.AllInbox, badgeCount = null))
        addAll(createTabPrimaryPreviewParam(icon = Icons.Outlined.AllInbox, badgeCount = 1))
        addAll(createTabPrimaryPreviewParam(icon = Icons.Outlined.AllInbox, badgeCount = 10))
        addAll(createTabPrimaryPreviewParam(icon = Icons.Outlined.AllInbox, badgeCount = 100))
        addAll(createTabPrimaryPreviewParam(icon = Icons.Outlined.AllInbox, badgeCount = 1000))
    },
)

private fun createTabPrimaryPreviewParam(
    icon: ImageVector?,
    badgeCount: Int?,
): Set<TabPrimaryPreviewParam> = setOf(
    TabPrimaryPreviewParam(
        active = true,
        title = "Active TabPrimary",
        enabled = true,
        icon = icon,
        badgeCount = badgeCount,
    ),
    TabPrimaryPreviewParam(
        active = false,
        title = "Inactive TabPrimary",
        enabled = true,
        icon = icon,
        badgeCount = badgeCount,
    ),
    TabPrimaryPreviewParam(
        active = true,
        title = "Disabled TabPrimary",
        enabled = false,
        icon = icon,
        badgeCount = badgeCount,
    ),
    TabPrimaryPreviewParam(
        active = false,
        title = "Active TabPrimary",
        enabled = false,
        icon = icon,
        badgeCount = badgeCount,
    ),
)

private data class TabPrimaryPreviewParam(
    val active: Boolean,
    val title: String,
    val enabled: Boolean,
    val icon: ImageVector?,
    val badgeCount: Int?,
)
