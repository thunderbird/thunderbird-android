package app.k9mail.core.ui.compose.designsystem.atom.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.PreviewWithThemes
import androidx.compose.material.Text as MaterialText

@Composable
fun TextCaption(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    MaterialText(
        text = text,
        style = MainTheme.typography.caption,
        modifier = modifier,
        color = color,
    )
}

@Preview(showBackground = true)
@Composable
internal fun TextCaptionPreview() {
    PreviewWithThemes {
        TextCaption(text = "TextCaption")
    }
}
