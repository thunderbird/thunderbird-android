package net.thunderbird.core.ui.setting.dialog.ui.components.list.decoration

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import net.thunderbird.core.ui.setting.SettingDecoration

@Composable
@Preview(showBackground = true)
internal fun SectionHeaderItemPreview() {
    PreviewWithThemes {
        SectionHeaderItem(
            setting = SettingDecoration.SectionHeader(
                id = "sectionHeader1",
                title = { "Section Header" },
            ),
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun SectionHeaderItemWithColorPreview() {
    PreviewWithThemes {
        SectionHeaderItem(
            setting = SettingDecoration.SectionHeader(
                id = "sectionHeader2",
                title = { "Section Header with Color" },
                color = { Color.Red },
            ),
        )
    }
}
