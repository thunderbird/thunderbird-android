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
internal fun TextDisplaySmallPreview() {
    PreviewWithThemes {
        TextDisplaySmall(
            text = "Text Display Small",
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextDisplaySmallWithAnnotatedStringPreview() {
    PreviewWithThemes {
        TextDisplaySmall(
            text = buildAnnotatedString {
                append("Text Display Small ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("Annotated")
                }
            },
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextDisplaySmallWithColorPreview() {
    PreviewWithThemes {
        TextDisplaySmall(
            text = "Text Display Small with color",
            color = MainTheme.colors.primary,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextDisplaySmallWithTextAlignPreview() {
    PreviewWithThemes {
        TextDisplaySmall(
            text = "Text Display Small with TextAlign End",
            textAlign = TextAlign.End,
        )
    }
}
