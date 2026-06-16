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
import net.thunderbird.components.ui.bolt.theme.BoltTheme

@Composable
fun TextHeadlineSmall(
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
        style = BoltTheme.typography.headlineSmall,
    )
}

@Composable
fun TextHeadlineSmall(
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
        style = BoltTheme.typography.headlineSmall,
    )
}

@Composable
@Preview(showBackground = true)
internal fun TextHeadlineSmallPreview() {
    PreviewWithThemes {
        TextHeadlineSmall(
            text = "Text Headline Small",
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextHeadlineSmallWithAnnotatedStringPreview() {
    PreviewWithThemes {
        TextHeadlineSmall(
            text = buildAnnotatedString {
                append("Text Headline Small ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("Annotated")
                }
            },
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextHeadlineSmallWithColorPreview() {
    PreviewWithThemes {
        TextHeadlineSmall(
            text = "Text Headline Small with color",
            color = BoltTheme.colors.primary,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextHeadlineSmallWithTextAlignPreview() {
    PreviewWithThemes {
        TextHeadlineSmall(
            text = "Text Headline Small with TextAlign End",
            textAlign = TextAlign.End,
        )
    }
}
