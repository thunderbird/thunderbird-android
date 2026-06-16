package net.thunderbird.components.ui.bolt.atom.text

import androidx.compose.material3.Text as Material3Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.theme.MainTheme

@Composable
fun TextHeadlineLarge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
) {
    Material3Text(
        text = text,
        modifier = modifier,
        color = color,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        style = MainTheme.typography.headlineLarge,
    )
}

@Composable
fun TextHeadlineLarge(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
) {
    Material3Text(
        text = text,
        modifier = modifier,
        color = color,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        style = MainTheme.typography.headlineLarge,
    )
}

@Composable
@Preview(showBackground = true)
internal fun TextHeadlineLargePreview() {
    PreviewWithThemes {
        TextHeadlineLarge(
            text = "Text Headline Large",
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextHeadlineLargeWithAnnotatedStringPreview() {
    PreviewWithThemes {
        TextHeadlineLarge(
            text = buildAnnotatedString {
                append("Text Headline Large ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("Annotated")
                }
            },
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextHeadlineLargeWithColorPreview() {
    PreviewWithThemes {
        TextHeadlineLarge(
            text = "Text Headline Large with color",
            color = MainTheme.colors.primary,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextHeadlineLargeWithTextAlignPreview() {
    PreviewWithThemes {
        TextHeadlineLarge(
            text = "Text Headline Large with TextAlign End",
            textAlign = TextAlign.End,
        )
    }
}
