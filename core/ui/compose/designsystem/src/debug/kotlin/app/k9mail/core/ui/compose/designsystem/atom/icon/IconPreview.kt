package app.k9mail.core.ui.compose.designsystem.atom.icon

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons

@Composable
@PreviewLightDark
internal fun IconPreview() {
    Icon(
        imageVector = Icons.Filled.Star,
        contentDescription = "Star Icon",
    )
}

@Composable
@PreviewLightDark
internal fun IconWithTintPreview() {
    Icon(
        imageVector = Icons.Filled.Star,
        tint = Color.Red,
        contentDescription = "Star Icon",
    )
}
