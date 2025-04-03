package app.k9mail.feature.account.avatar.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes

@Composable
@Preview(showBackground = true)
internal fun AvatarOutlinedPreview() {
    PreviewWithThemes {
        AvatarOutlined(
            color = Color(0xFFe57373),
            name = "example",
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AvatarOutlinedLargePreview() {
    PreviewWithThemes {
        AvatarOutlined(
            color = Color(0xFFe57373),
            name = "example",
            size = AvatarSize.LARGE,
        )
    }
}
