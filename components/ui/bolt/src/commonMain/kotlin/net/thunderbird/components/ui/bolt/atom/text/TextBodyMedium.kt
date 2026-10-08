package net.thunderbird.components.ui.bolt.atom.text

import androidx.compose.material3.Text as Material3Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.theme.BoltTheme

@Composable
fun TextBodyMedium(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {},
) {
    Material3Text(
        text = text,
        modifier = modifier,
        color = color,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        style = BoltTheme.typography.bodyMedium,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun TextBodyMedium(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {},
) {
    Material3Text(
        text = text,
        modifier = modifier,
        color = color,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        style = BoltTheme.typography.bodyMedium,
        onTextLayout = onTextLayout,
    )
}

@Composable
@Preview(showBackground = true)
internal fun TextBodyMediumPreview() {
    PreviewWithThemes {
        TextBodyMedium(
            text = "Text Body Medium",
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextBodyMediumWithAnnotatedStringPreview() {
    PreviewWithThemes {
        TextBodyMedium(
            text = buildAnnotatedString {
                append("Text Body Medium ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("Annotated")
                }
            },
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextBodyMediumWithColorPreview() {
    PreviewWithThemes {
        TextBodyMedium(
            text = "Text Body Medium with color",
            color = BoltTheme.colors.primary,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextBodyMediumWithTextAlignPreview() {
    PreviewWithThemes {
        TextBodyMedium(
            text = "Text Body Medium with TextAlign End",
            textAlign = TextAlign.End,
        )
    }
}
