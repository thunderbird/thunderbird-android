package app.k9mail.core.ui.compose.designsystem.molecule.input

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes

@Composable
@Preview(showBackground = true)
internal fun AdvancedTextInputPreview() {
    PreviewWithThemes {
        AdvancedTextInput(
            onTextChange = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AdvancedTextInputIsRequiredPreview() {
    PreviewWithThemes {
        AdvancedTextInput(
            onTextChange = {},
            label = "Text input is required",
            isRequired = true,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AdvancedTextInputWithErrorPreview() {
    PreviewWithThemes {
        AdvancedTextInput(
            onTextChange = {},
            errorMessage = "Text input error",
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AdvancedTextInputWithAnnotatedStringPreview() {
    PreviewWithThemes {
        AdvancedTextInput(
            onTextChange = {},
            text = TextFieldValue(
                annotatedString = buildAnnotatedString {
                    append("Text input with ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Annotated")
                    }
                },
            ),
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AdvancedTextInputWithSelectionPreview() {
    PreviewWithThemes {
        AdvancedTextInput(
            onTextChange = {},
            text = TextFieldValue("Text input with selection", selection = TextRange(0, 4)),
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AdvancedTextInputWithCompositionPreview() {
    PreviewWithThemes {
        AdvancedTextInput(
            onTextChange = {},
            text = TextFieldValue(
                text = "Text input with composition",
                composition = TextRange(0, 4),
            ),
        )
    }
}
