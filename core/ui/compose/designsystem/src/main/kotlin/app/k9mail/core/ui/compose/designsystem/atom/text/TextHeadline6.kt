package app.k9mail.core.ui.compose.designsystem.atom.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.PreviewWithThemes
import androidx.compose.material.Text as MaterialText

@Composable
fun TextHeadline6(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
) {
    MaterialText(
        text = text,
        modifier = modifier,
        color = color,
        textAlign = textAlign,
        style = MainTheme.typography.h6,
    )
}

@Composable
fun TextHeadline6(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
) {
    MaterialText(
        text = text,
        modifier = modifier,
        color = color,
        textAlign = textAlign,
        style = MainTheme.typography.h6,
    )
}

@Preview(showBackground = true)
@Composable
internal fun TextHeadline6Preview() {
    PreviewWithThemes {
        TextHeadline6(text = "TextHeadline6")
    }
}

@Preview(showBackground = true)
@Composable
internal fun TextHeadline6WithAnnotatedStringPreview() {
    PreviewWithThemes {
        TextHeadline6(
            text = buildAnnotatedString {
                append("Normal")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("Annotated")
                }
            },
        )
    }
}
