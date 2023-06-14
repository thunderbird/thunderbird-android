package app.k9mail.core.ui.compose.designsystem.atom.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.text.TextButton
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.PreviewWithThemes
import androidx.compose.material.OutlinedButton as MaterialOutlinedButton

@Composable
fun ButtonOutlined(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color? = null,
    contentPadding: PaddingValues = buttonContentPadding(),
) {
    MaterialOutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = color ?: MainTheme.colors.primary,
            backgroundColor = Color.Transparent,
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) {
                color ?: MainTheme.colors.primary
            } else {
                MainTheme.colors.onSurface.copy(
                    alpha = 0.12f,
                )
            },
        ),
        contentPadding = contentPadding,
    ) {
        TextButton(text = text)
    }
}

@Preview(showBackground = true)
@Composable
internal fun ButtonOutlinedPreview() {
    PreviewWithThemes {
        ButtonOutlined(
            text = "ButtonOutlined",
            onClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
internal fun ButtonOutlinedColoredPreview() {
    PreviewWithThemes {
        ButtonOutlined(
            text = "ButtonOutlinedColored",
            onClick = {},
            color = Color.Magenta,
        )
    }
}

@Preview(showBackground = true)
@Composable
internal fun ButtonOutlinedDisabledPreview() {
    PreviewWithThemes {
        ButtonOutlined(
            text = "ButtonOutlinedDisabled",
            onClick = {},
            enabled = false,
        )
    }
}
