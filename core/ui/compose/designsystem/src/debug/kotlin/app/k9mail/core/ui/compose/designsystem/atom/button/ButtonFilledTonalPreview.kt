package app.k9mail.core.ui.compose.designsystem.atom.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes

@Composable
@Preview(showBackground = true)
internal fun ButtonFilledTonalPreview() {
    PreviewWithThemes {
        ButtonFilledTonal(
            text = "Button Filled Tonal",
            onClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ButtonFilledTonalDisabledPreview() {
    PreviewWithThemes {
        ButtonFilledTonal(
            text = "Button Filled Tonal Disabled",
            onClick = {},
            enabled = false,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ButtonFilledTonalMultiLinePreview() {
    PreviewWithThemes {
        ButtonFilledTonal(
            text = "First\nSecond line",
            onClick = {},
        )
    }
}
