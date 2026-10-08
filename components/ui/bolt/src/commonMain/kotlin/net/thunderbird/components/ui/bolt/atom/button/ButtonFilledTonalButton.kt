package net.thunderbird.components.ui.bolt.atom.button

import androidx.compose.material3.FilledTonalButton as Material3FilledTonalButton
import androidx.compose.material3.Text as Material3Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes

@Composable
fun ButtonFilledTonal(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Material3FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    ) {
        Material3Text(
            text = text,
            textAlign = TextAlign.Center,
        )
    }
}

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
