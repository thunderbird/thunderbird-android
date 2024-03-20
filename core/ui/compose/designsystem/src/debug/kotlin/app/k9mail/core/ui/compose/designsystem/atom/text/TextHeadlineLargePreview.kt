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
