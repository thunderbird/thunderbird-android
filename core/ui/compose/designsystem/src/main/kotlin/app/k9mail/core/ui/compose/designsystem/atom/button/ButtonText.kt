package app.k9mail.core.ui.compose.designsystem.atom.button

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.atom.text.TextButton
import app.k9mail.core.ui.compose.theme.PreviewWithThemes
import androidx.compose.material.TextButton as MaterialTextButton

@Composable
fun ButtonText(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = buttonContentPadding(),
) {
    MaterialTextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(),
        contentPadding = contentPadding,
    ) {
        TextButton(text = text)
    }
}

@Preview(showBackground = true)
@Composable
internal fun ButtonTextPreview() {
    PreviewWithThemes {
        ButtonText(
            text = "ButtonText",
            onClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
internal fun ButtonTextDisabledPreview() {
    PreviewWithThemes {
        ButtonText(
            text = "ButtonTextDisabled",
            onClick = {},
            enabled = false,
        )
    }
}
