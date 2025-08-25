package net.thunderbird.feature.navigation.drawer.siderail.ui.account

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import net.thunderbird.feature.navigation.drawer.dropdown.ui.FakeData.MAIL_DISPLAY_ACCOUNT

@Composable
@Preview(showBackground = true)
internal fun SideRailAccountListItemPreview() {
    PreviewWithThemes {
        SideRailAccountListItem(
            account = MAIL_DISPLAY_ACCOUNT,
            onClick = { },
            selected = false,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun SideRailAccountListItemSelectedPreview() {
    PreviewWithThemes {
        SideRailAccountListItem(
            account = MAIL_DISPLAY_ACCOUNT,
            onClick = { },
            selected = true,
        )
    }
}
