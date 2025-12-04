package net.thunderbird.feature.account.avatar.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import net.thunderbird.feature.account.avatar.Avatar

@Composable
@PreviewLightDark
internal fun AvatarPreview() {
    PreviewWithTheme {
        Avatar(
            avatar = Avatar.Monogram("AB"),
            color = Color.Red,
            size = AvatarSize.MEDIUM,
        )
    }
}

@Composable
@PreviewLightDark
internal fun AvatarSelectedPreview() {
    PreviewWithTheme {
        Avatar(
            avatar = Avatar.Monogram("AB"),
            color = Color.Red,
            size = AvatarSize.MEDIUM,
            selected = true,
        )
    }
}

@Composable
@PreviewLightDark
internal fun AvatarIconPreview() {
    PreviewWithTheme {
        Avatar(
            avatar = Avatar.Icon("person"),
            color = Color.Red,
            size = AvatarSize.MEDIUM,
        )
    }
}

@Composable
@PreviewLightDark
internal fun AvatarIconLargePreview() {
    PreviewWithTheme {
        Avatar(
            avatar = Avatar.Icon("person"),
            color = Color.Red,
            size = AvatarSize.LARGE,
        )
    }
}
