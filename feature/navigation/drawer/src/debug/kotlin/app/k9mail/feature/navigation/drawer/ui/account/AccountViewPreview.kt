package app.k9mail.feature.navigation.drawer.ui.account

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes

@Composable
@Preview(showBackground = true)
internal fun AccountViewPreview() {
    PreviewWithThemes {
        AccountView(
            displayName = DISPLAY_NAME,
            emailAddress = EMAIL_ADDRESS,
            accountColor = 0,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AccountViewWithColorPreview() {
    PreviewWithThemes {
        AccountView(
            displayName = DISPLAY_NAME,
            emailAddress = EMAIL_ADDRESS,
            accountColor = 0xFF0000,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AccountViewWithLongDisplayName() {
    PreviewWithThemes {
        AccountView(
            displayName = "$LONG_TEXT $DISPLAY_NAME",
            emailAddress = EMAIL_ADDRESS,
            accountColor = 0,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AccountViewWithLongEmailPreview() {
    PreviewWithThemes {
        AccountView(
            displayName = DISPLAY_NAME,
            emailAddress = "$LONG_TEXT@example.com",
            accountColor = 0,
        )
    }
}

private const val DISPLAY_NAME = "Account Name"

private const val EMAIL_ADDRESS = "test@example.com"

private const val LONG_TEXT = "loremipsumdolorsitametconsetetursadipscingelitr" +
    "seddiamnonumyeirmodtemporinviduntutlaboreetdoloremagnaaliquyameratseddiamvoluptua"
