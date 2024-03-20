package app.k9mail.core.ui.compose.designsystem.atom.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import app.k9mail.core.ui.compose.theme2.MainTheme

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
