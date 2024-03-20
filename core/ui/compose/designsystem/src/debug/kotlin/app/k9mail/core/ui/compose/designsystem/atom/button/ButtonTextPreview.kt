package app.k9mail.core.ui.compose.designsystem.atom.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes

@Composable
@Preview(showBackground = true)
internal fun ButtonTextPreview() {
    PreviewWithThemes {
        ButtonText(
            text = "Button Text",
            onClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ButtonTextColoredPreview() {
    PreviewWithThemes {
        ButtonText(
            text = "Button Text Colored",
            onClick = {},
            color = Color.Magenta,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ButtonTextDisabledPreview() {
    PreviewWithThemes {
        ButtonText(
            text = "Button Text Disabled",
            onClick = {},
            enabled = false,
        )
    }
}
