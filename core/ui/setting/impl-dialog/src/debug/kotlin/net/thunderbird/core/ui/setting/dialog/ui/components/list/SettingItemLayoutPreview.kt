package net.thunderbird.core.ui.setting.dialog.ui.components.list

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleLarge

@Composable
@Preview(showBackground = true)
internal fun SettingItemLayoutPreview() {
    PreviewWithThemes {
        SettingItemLayout(
            onClick = {},
            icon = null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            TextTitleLarge(text = "PreferenceItemLayoutContent")
        }
    }
}

@Composable
@Preview(showBackground = true)
internal fun SettingItemLayoutWithIconPreview() {
    PreviewWithThemes {
        SettingItemLayout(
            onClick = {},
            icon = Icons.Outlined.Info,
            modifier = Modifier.fillMaxWidth(),
        ) {
            TextTitleLarge(text = "PreferenceItemLayoutContent")
        }
    }
}
