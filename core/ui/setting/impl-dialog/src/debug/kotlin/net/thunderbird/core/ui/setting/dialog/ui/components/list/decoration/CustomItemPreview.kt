package net.thunderbird.core.ui.setting.dialog.ui.components.list.decoration

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleLarge
import net.thunderbird.core.ui.setting.SettingDecoration

@Composable
@Preview(showBackground = true)
internal fun CustomItemPreview() {
    PreviewWithThemes {
        CustomItem(
            setting = SettingDecoration.Custom(
                id = "custom_decoration",
                customUi = { modifier ->
                    TextTitleLarge("Custom Decoration")
                },
            ),
        )
    }
}
