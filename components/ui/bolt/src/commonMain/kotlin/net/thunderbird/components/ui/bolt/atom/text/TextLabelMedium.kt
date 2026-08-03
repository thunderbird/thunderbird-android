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
fun TextLabelMedium(
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
        style = BoltTheme.typography.labelMedium,
    )
}

@Composable
fun TextLabelMedium(
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
        style = BoltTheme.typography.labelMedium,
    )
}

@Composable
@Preview(showBackground = true)
internal fun TextLabelMediumPreview() {
    PreviewWithThemes {
        TextLabelMedium(
            text = "Text Label Medium",
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextLabelMediumWithAnnotatedStringPreview() {
    PreviewWithThemes {
        TextLabelMedium(
            text = buildAnnotatedString {
                append("Text Label Medium ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("Annotated")
                }
            },
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextLabelMediumWithColorPreview() {
    PreviewWithThemes {
        TextLabelMedium(
            text = "Text Label Medium with color",
            color = BoltTheme.colors.primary,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextLabelMediumWithTextAlignPreview() {
    PreviewWithThemes {
        TextLabelMedium(
            text = "Text Label Medium with TextAlign End",
            textAlign = TextAlign.End,
        )
    }
}
