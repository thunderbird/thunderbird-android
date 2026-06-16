package net.thunderbird.components.ui.bolt.atom.button

import androidx.compose.material3.Text as Material3Text
import androidx.compose.material3.TextButton as Material3TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.theme.BoltTheme

@Composable
fun ButtonText(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    Material3TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = color ?: BoltTheme.colors.primary,
        ).toMaterial3Colors(),
    ) {
        leadingIcon?.invoke()
        Material3Text(
            text = text,
            textAlign = TextAlign.Center,
        )
    }
}

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

@Composable
@Preview(showBackground = true)
internal fun ButtonTextMultiLinePreview() {
    PreviewWithThemes {
        ButtonText(
            text = "First\nSecond line",
            onClick = {},
        )
    }
}
