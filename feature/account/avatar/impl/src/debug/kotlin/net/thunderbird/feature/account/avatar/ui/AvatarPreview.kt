package net.thunderbird.feature.account.avatar.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes

@Composable
@Preview(showBackground = true)
internal fun AvatarPreview() {
    PreviewWithThemes {
        Avatar(
            color = Color(0xFFe57373),
            name = "example",
            selected = false,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AvatarSelectedPreview() {
    PreviewWithThemes {
        Avatar(
            color = Color(0xFFe57373),
            name = "example",
            selected = true,
        )
    }
}
