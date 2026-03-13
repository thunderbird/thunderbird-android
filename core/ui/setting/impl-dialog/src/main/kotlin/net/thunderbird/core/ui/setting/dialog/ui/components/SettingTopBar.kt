package net.thunderbird.core.ui.setting.dialog.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.designsystem.organism.SubtitleTopAppBarWithBackButton
import app.k9mail.core.ui.compose.designsystem.organism.TopAppBarWithBackButton

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
