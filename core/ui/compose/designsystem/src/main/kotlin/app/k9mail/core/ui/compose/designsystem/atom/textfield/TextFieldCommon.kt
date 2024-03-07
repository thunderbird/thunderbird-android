package app.k9mail.core.ui.compose.designsystem.atom.textfield

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.theme.PreviewWithThemes

private val LINE_BREAK = "[\\r\\n]".toRegex()

internal fun stripLineBreaks(onValueChange: (String) -> Unit): (String) -> Unit = { value ->
    onValueChange(value.replace(LINE_BREAK, replacement = ""))
}

internal fun selectLabel(
    label: String?,
    isRequired: Boolean,
): @Composable (() -> Unit)? {
    return if (label != null || isRequired) {
        {
            TextFieldLabel(label.orEmpty(), isRequired)
        }
    } else {
        null
    }
}

@Composable
internal fun TextFieldLabel(
    label: String,
    isRequired: Boolean,
) {
    Text(
        text = if (isRequired) {
            "$label$ASTERISK"
        } else {
            label
        },
    )
}

private const val ASTERISK = "*"

@Preview(showBackground = true)
@Composable
internal fun TextFieldLabelPreview() {
    PreviewWithThemes {
        TextFieldLabel(
            label = "Label",
            isRequired = false,
        )
    }
}

@Preview(showBackground = true)
@Composable
internal fun TextFieldLabelRequiredPreview() {
    PreviewWithThemes {
        TextFieldLabel(
            label = "Label",
            isRequired = true,
        )
    }
}

@Preview(showBackground = true)
@Composable
internal fun TextFieldLabelRequiredEmptyLabelPreview() {
    PreviewWithThemes {
        TextFieldLabel(
            label = "",
            isRequired = true,
        )
    }
}
