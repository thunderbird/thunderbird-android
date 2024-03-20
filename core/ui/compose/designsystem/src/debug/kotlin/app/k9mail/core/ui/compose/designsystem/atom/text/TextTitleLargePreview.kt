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
internal fun TextTitleLargePreview() {
    PreviewWithThemes {
        TextTitleLarge(
            text = "Text Title Large",
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextTitleLargeWithAnnotatedStringPreview() {
    PreviewWithThemes {
        TextTitleLarge(
            text = buildAnnotatedString {
                append("Text Title Large ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("Annotated")
                }
            },
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextTitleLargeWithColorPreview() {
    PreviewWithThemes {
        TextTitleLarge(
            text = "Text Title Large with color",
            color = MainTheme.colors.primary,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextTitleLargeWithTextAlignPreview() {
    PreviewWithThemes {
        TextTitleLarge(
            text = "Text Title Large with TextAlign End",
            textAlign = TextAlign.End,
        )
    }
}
