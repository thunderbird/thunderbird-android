package net.thunderbird.components.ui.bolt.atom.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes

@Composable
@Preview(showBackground = true)
internal fun ButtonFilledPreview() {
    PreviewWithThemes {
        ButtonFilled(
            text = "Button Filled",
            onClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ButtonFilledDisabledPreview() {
    PreviewWithThemes {
        ButtonFilled(
            text = "Button Filled Disabled",
            onClick = {},
            enabled = false,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ButtonFilledMultiLinePreview() {
    PreviewWithThemes {
        ButtonFilled(
            text = "First\nSecond line",
            onClick = {},
        )
    }
}
