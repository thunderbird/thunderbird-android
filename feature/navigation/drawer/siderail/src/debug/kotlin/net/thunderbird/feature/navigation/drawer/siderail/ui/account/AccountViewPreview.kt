package net.thunderbird.feature.navigation.drawer.siderail.ui.account

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import net.thunderbird.feature.navigation.drawer.siderail.ui.FakeData.DISPLAY_ACCOUNT

@Composable
@Preview(showBackground = true)
internal fun AccountViewPreview() {
    PreviewWithThemes {
        AccountView(
            account = DISPLAY_ACCOUNT,
            onClick = {},
            showAvatar = false,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AccountViewWithColorPreview() {
    PreviewWithThemes {
        AccountView(
            account = DISPLAY_ACCOUNT,
            onClick = {},
            showAvatar = false,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AccountViewWithLongDisplayName() {
    PreviewWithThemes {
        AccountView(
            account = DISPLAY_ACCOUNT,
            onClick = {},
            showAvatar = false,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AccountViewWithLongEmailPreview() {
    PreviewWithThemes {
        AccountView(
            account = DISPLAY_ACCOUNT,
            onClick = {},
            showAvatar = false,
        )
    }
}
