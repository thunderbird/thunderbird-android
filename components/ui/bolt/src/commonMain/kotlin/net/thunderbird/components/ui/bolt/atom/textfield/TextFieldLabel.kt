package net.thunderbird.components.ui.bolt.atom.textfield

import androidx.compose.material3.Text as Material3Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes

private const val ASTERISK = "*"

@Composable
internal fun TextFieldLabel(
    label: String,
    isRequired: Boolean,
) {
    Material3Text(
        text = if (isRequired) {
            "$label$ASTERISK"
        } else {
            label
        },
    )
}

@Composable
@Preview(showBackground = true)
internal fun TextFieldLabelPreview() {
    PreviewWithThemes {
        TextFieldLabel(
            label = "Label",
            isRequired = false,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextFieldLabelRequiredPreview() {
    PreviewWithThemes {
        TextFieldLabel(
            label = "Label",
            isRequired = true,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextFieldLabelRequiredEmptyLabelPreview() {
    PreviewWithThemes {
        TextFieldLabel(
            label = "",
            isRequired = true,
        )
    }
}
