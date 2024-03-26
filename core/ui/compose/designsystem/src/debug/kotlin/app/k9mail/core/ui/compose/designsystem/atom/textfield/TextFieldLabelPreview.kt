package app.k9mail.core.ui.compose.designsystem.atom.textfield

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes

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
