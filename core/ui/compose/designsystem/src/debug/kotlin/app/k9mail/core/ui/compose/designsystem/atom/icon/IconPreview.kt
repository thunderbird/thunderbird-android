package app.k9mail.core.ui.compose.designsystem.atom.icon

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes

@Preview(showBackground = true)
@Composable
internal fun IconPreview() {
    PreviewWithThemes {
        Icon(
            imageVector = Icons.Filled.error,
        )
    }
}

@Preview(showBackground = true)
@Composable
internal fun IconTintedPreview() {
    PreviewWithThemes {
        Icon(
            imageVector = Icons.Filled.error,
            tint = Color.Magenta,
        )
    }
}
