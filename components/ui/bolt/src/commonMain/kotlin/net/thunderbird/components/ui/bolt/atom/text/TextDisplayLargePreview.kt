package net.thunderbird.components.ui.bolt.atom.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.theme.MainTheme

@Composable
@Preview(showBackground = true)
internal fun TextDisplayLargePreview() {
    PreviewWithThemes {
        TextDisplayLarge(
            text = "Text Display Large",
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextDisplayLargeWithAnnotatedStringPreview() {
    PreviewWithThemes {
        TextDisplayLarge(
            text = buildAnnotatedString {
                append("Text Display Large ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("Annotated")
                }
            },
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextDisplayLargeWithColorPreview() {
    PreviewWithThemes {
        TextDisplayLarge(
            text = "Text Display Large with color",
            color = MainTheme.colors.primary,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextDisplayLargeWithTextAlignPreview() {
    PreviewWithThemes {
        TextDisplayLarge(
            text = "Text Display Large with TextAlign End",
            textAlign = TextAlign.End,
        )
    }
}
