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
            color = MainTheme.colors.primary,
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
