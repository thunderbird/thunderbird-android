package net.thunderbird.core.ui.compose.designsystem.atom.icon

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark

@Composable
@PreviewLightDark
internal fun BadgeIconPreview() {
    BadgeIcon(
        imageVector = BadgeIcons.Filled.NewMail,
        contentDescription = "NewMail Badge",
    )
}

@Composable
@PreviewLightDark
internal fun BadgeIconWithTintPreview() {
    BadgeIcon(
        imageVector = BadgeIcons.Filled.NewMail,
        tint = Color.Red,
        contentDescription = "NewMail Badge",
    )
}
