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
internal fun TextBodyMediumPreview() {
    PreviewWithThemes {
        TextBodyMedium(
            text = "Text Body Medium",
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextBodyMediumWithAnnotatedStringPreview() {
    PreviewWithThemes {
        TextBodyMedium(
            text = buildAnnotatedString {
                append("Text Body Medium ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("Annotated")
                }
            },
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextBodyMediumWithColorPreview() {
    PreviewWithThemes {
        TextBodyMedium(
            text = "Text Body Medium with color",
            color = MainTheme.colors.primary,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextBodyMediumWithTextAlignPreview() {
    PreviewWithThemes {
        TextBodyMedium(
            text = "Text Body Medium with TextAlign End",
            textAlign = TextAlign.End,
        )
    }
}
