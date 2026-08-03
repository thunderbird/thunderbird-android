package net.thunderbird.core.ui.setting.dialog.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import net.thunderbird.components.ui.bolt.organism.SubtitleTopAppBarWithBackButton
import net.thunderbird.components.ui.bolt.organism.TopAppBarWithBackButton

@Composable
internal fun SettingTopBar(
    title: String,
    subtitle: String?,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    if (subtitle != null) {
        SubtitleTopAppBarWithBackButton(
            title = title,
            subtitle = subtitle,
            onBackClick = onBack,
            actions = actions,
        )
    } else {
        TopAppBarWithBackButton(
            title = title,
            onBackClick = onBack,
        )
    }
}
