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
