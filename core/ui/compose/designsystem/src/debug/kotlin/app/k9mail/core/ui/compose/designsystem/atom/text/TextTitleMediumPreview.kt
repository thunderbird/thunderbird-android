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
internal fun TextTitleMediumPreview() {
    PreviewWithThemes {
        TextTitleMedium(
            text = "Text Title Medium",
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextTitleMediumWithAnnotatedStringPreview() {
    PreviewWithThemes {
        TextTitleMedium(
            text = buildAnnotatedString {
                append("Text Title Medium ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("Annotated")
                }
            },
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextTitleMediumWithColorPreview() {
    PreviewWithThemes {
        TextTitleMedium(
            text = "Text Title Medium with color",
            color = MainTheme.colors.primary,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextTitleMediumWithTextAlignPreview() {
    PreviewWithThemes {
        TextTitleMedium(
            text = "Text Title Medium with TextAlign End",
            textAlign = TextAlign.End,
        )
    }
}
