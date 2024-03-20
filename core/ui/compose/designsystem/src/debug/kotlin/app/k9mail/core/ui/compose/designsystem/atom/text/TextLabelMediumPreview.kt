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
            color = MainTheme.colors.primary,
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
