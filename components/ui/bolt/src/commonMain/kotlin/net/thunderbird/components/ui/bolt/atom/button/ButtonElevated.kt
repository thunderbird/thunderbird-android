package net.thunderbird.components.ui.bolt.atom.button

import androidx.compose.material3.ElevatedButton as Material3ElevatedButton
import androidx.compose.material3.Text as Material3Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes

@Composable
fun ButtonElevated(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Material3ElevatedButton(
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
internal fun ButtonElevatedPreview() {
    PreviewWithThemes {
        ButtonElevated(
            text = "Button Elevated",
            onClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ButtonElevatedDisabledPreview() {
    PreviewWithThemes {
        ButtonElevated(
            text = "Button Elevated Disabled",
            onClick = {},
            enabled = false,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ButtonElevatedMultiLinePreview() {
    PreviewWithThemes {
        ButtonElevated(
            text = "First\nSecond line",
            onClick = {},
        )
    }
}
