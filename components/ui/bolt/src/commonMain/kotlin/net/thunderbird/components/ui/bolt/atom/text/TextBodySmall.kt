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
import net.thunderbird.components.ui.bolt.theme.MainTheme

@Composable
fun TextBodySmall(
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
        style = MainTheme.typography.bodySmall,
    )
}

@Composable
fun TextBodySmall(
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
        style = MainTheme.typography.bodySmall,
        inlineContent = inlineContent,
    )
}

@Composable
@Preview(showBackground = true)
internal fun TextBodySmallPreview() {
    PreviewWithThemes {
        TextBodySmall(
            text = "Text Body Small",
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextBodySmallWithAnnotatedStringPreview() {
    PreviewWithThemes {
        TextBodySmall(
            text = buildAnnotatedString {
                append("Text Body Small ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("Annotated")
                }
            },
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextBodySmallWithColorPreview() {
    PreviewWithThemes {
        TextBodySmall(
            text = "Text Body Small with color",
            color = MainTheme.colors.primary,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextBodySmallWithTextAlignPreview() {
    PreviewWithThemes {
        TextBodySmall(
            text = "Text Body Small with TextAlign End",
            textAlign = TextAlign.End,
        )
    }
}
