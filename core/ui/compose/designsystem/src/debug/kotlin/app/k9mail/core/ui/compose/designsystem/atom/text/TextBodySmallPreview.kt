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
