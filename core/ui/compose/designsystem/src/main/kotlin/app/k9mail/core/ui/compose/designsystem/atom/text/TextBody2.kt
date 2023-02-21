package app.k9mail.core.ui.compose.designsystem.atom.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.PreviewWithThemes
import androidx.compose.material.Text as MaterialText

@Composable
fun TextBody2(
    text: String,
    modifier: Modifier = Modifier,
) {
    MaterialText(
        text = text,
        style = MainTheme.typography.body2,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
internal fun TextBody2Preview() {
    PreviewWithThemes {
        TextBody2(text = "TextBody2")
    }
}
