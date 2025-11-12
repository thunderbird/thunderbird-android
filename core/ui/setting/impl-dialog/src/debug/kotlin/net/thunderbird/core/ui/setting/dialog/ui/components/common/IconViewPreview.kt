package net.thunderbird.core.ui.setting.dialog.ui.components.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons
import net.thunderbird.core.ui.setting.SettingValue.IconList.IconOption

@Composable
@Preview(showBackground = true)
internal fun IconViewPreview() {
    PreviewWithTheme {
        IconView(
            icon = IconOption(
                id = "icon1",
                icon = { Icons.Outlined.Person },
            ),
            color = Color.Red,
            onClick = { },
        )
    }
}

@Composable
@PreviewLightDark
internal fun IconViewSelectedPreview() {
    PreviewWithTheme {
        IconView(
            icon = IconOption(
                id = "icon1",
                icon = { Icons.Outlined.Person },
            ),
            color = Color.Red,
            onClick = { },
            isSelected = true,
        )
    }
}
