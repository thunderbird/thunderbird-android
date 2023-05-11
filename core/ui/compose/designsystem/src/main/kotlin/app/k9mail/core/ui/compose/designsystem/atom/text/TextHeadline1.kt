package app.k9mail.core.ui.compose.designsystem.atom.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.PreviewWithThemes
import androidx.compose.material.Text as MaterialText

@Composable
fun TextHeadline1(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    MaterialText(
        text = text,
        style = MainTheme.typography.h1,
        modifier = modifier,
        color = color,
    )
}

@Composable
fun TextHeadline1(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    MaterialText(
        text = text,
        style = MainTheme.typography.h1,
        modifier = modifier,
        color = color,
    )
}

@Preview(showBackground = true)
@Composable
internal fun TextHeadline1Preview() {
    PreviewWithThemes {
        TextHeadline1(text = "TextHeadline1")
    }
}

@Preview(showBackground = true)
@Composable
internal fun TextHeadline1WithAnnotatedStringPreview() {
    PreviewWithThemes {
        TextHeadline1(
            text = buildAnnotatedString {
                append("Normal")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("Annotated")
                }
            },
        )
    }
}
