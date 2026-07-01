package net.thunderbird.core.ui.setting.component.list.item

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.atom.text.TextTitleLarge

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
