package net.thunderbird.components.ui.bolt.atom.text

import androidx.compose.foundation.text.InlineTextContent
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
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.theme.BoltTheme

@Composable
fun TextLabelLarge(
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
        style = BoltTheme.typography.labelLarge,
    )
}

@Composable
fun TextLabelLarge(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    inlineContent: ImmutableMap<String, InlineTextContent> = persistentMapOf(),
) {
    Material3Text(
        text = text,
        modifier = modifier,
        color = color,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        style = BoltTheme.typography.labelLarge,
        inlineContent = inlineContent,
    )
}

@Composable
@Preview(showBackground = true)
internal fun TextLabelLargePreview() {
    PreviewWithThemes {
        TextLabelLarge(
            text = "Text Label Large",
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextLabelLargeWithAnnotatedStringPreview() {
    PreviewWithThemes {
        TextLabelLarge(
            text = buildAnnotatedString {
                append("Text Label Large ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("Annotated")
                }
            },
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextLabelLargeWithColorPreview() {
    PreviewWithThemes {
        TextLabelLarge(
            text = "Text Label Large with color",
            color = BoltTheme.colors.primary,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextLabelLargeWithTextAlignPreview() {
    PreviewWithThemes {
        TextLabelLarge(
            text = "Text Label Large with TextAlign End",
            textAlign = TextAlign.End,
        )
    }
}
