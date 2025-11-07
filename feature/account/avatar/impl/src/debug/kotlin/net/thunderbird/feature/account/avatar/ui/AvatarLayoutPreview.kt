package net.thunderbird.feature.account.avatar.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme

@Composable
@PreviewLightDark
internal fun AvatarLayoutPreview() {
    PreviewWithTheme {
        AvatarLayout(
            color = Color.Yellow,
            backgroundColor = Color.White,
            size = AvatarSize.MEDIUM,
        ) { }
    }
}

@Composable
@PreviewLightDark
internal fun AvatarLayoutLargePreview() {
    PreviewWithTheme {
        AvatarLayout(
            color = Color.Red,
            backgroundColor = Color.White,
            size = AvatarSize.LARGE,
        ) { }
    }
}
