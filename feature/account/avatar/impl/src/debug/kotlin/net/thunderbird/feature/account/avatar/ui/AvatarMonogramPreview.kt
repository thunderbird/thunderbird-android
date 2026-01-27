package net.thunderbird.feature.account.avatar.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme

@Composable
@PreviewLightDark
internal fun AvatarMonogramPreview() {
    PreviewWithTheme {
        AvatarMonogram(
            monogram = "AB",
            color = Color.Red,
            size = AvatarSize.MEDIUM,
        )
    }
}

@Composable
@PreviewLightDark
internal fun AvatarMonogramLargePreview() {
    PreviewWithTheme {
        AvatarMonogram(
            monogram = "AB",
            color = Color.Red,
            size = AvatarSize.LARGE,
        )
    }
}
