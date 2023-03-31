package app.k9mail.core.ui.compose.designsystem.atom.button

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.atom.text.TextButton
import app.k9mail.core.ui.compose.theme.PreviewWithThemes
import androidx.compose.material.Button as MaterialButton

@Composable
fun Button(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = buttonContentPadding(),
) {
    MaterialButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(),
        contentPadding = contentPadding,
    ) {
        TextButton(text = text)
    }
}

@Preview(showBackground = true)
@Composable
internal fun ButtonPreview() {
    PreviewWithThemes {
        Button(
            text = "Button",
            onClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
internal fun ButtonDisabledPreview() {
    PreviewWithThemes {
        Button(
            text = "ButtonDisabled",
            onClick = {},
            enabled = false,
        )
    }
}
