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
internal fun TextDisplayMediumPreview() {
    PreviewWithThemes {
        TextDisplayMedium(
            text = "Text Display Medium",
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextDisplayMediumWithAnnotatedStringPreview() {
    PreviewWithThemes {
        TextDisplayMedium(
            text = buildAnnotatedString {
                append("Text Display Medium ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("Annotated")
                }
            },
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextDisplayMediumWithColorPreview() {
    PreviewWithThemes {
        TextDisplayMedium(
            text = "Text Display Medium with color",
            color = MainTheme.colors.primary,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextDisplayMediumWithTextAlignPreview() {
    PreviewWithThemes {
        TextDisplayMedium(
            text = "Text Display Medium with TextAlign End",
            textAlign = TextAlign.End,
        )
    }
}
