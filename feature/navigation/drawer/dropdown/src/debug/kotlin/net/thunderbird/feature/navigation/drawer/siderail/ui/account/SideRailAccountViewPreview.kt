package net.thunderbird.feature.navigation.drawer.siderail.ui.account

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import net.thunderbird.feature.navigation.drawer.dropdown.ui.FakeData.DISPLAY_ACCOUNT

@Composable
@Preview(showBackground = true)
internal fun SideRailAccountViewPreview() {
    PreviewWithThemes {
        SideRailAccountView(
            account = DISPLAY_ACCOUNT,
            onClick = {},
            showAvatar = false,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun SideRailAccountViewWithColorPreview() {
    PreviewWithThemes {
        SideRailAccountView(
            account = DISPLAY_ACCOUNT,
            onClick = {},
            showAvatar = false,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun SideRailAccountViewWithLongDisplayName() {
    PreviewWithThemes {
        SideRailAccountView(
            account = DISPLAY_ACCOUNT,
            onClick = {},
            showAvatar = false,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun SideRailAccountViewWithLongEmailPreview() {
    PreviewWithThemes {
        SideRailAccountView(
            account = DISPLAY_ACCOUNT,
            onClick = {},
            showAvatar = false,
        )
    }
}
