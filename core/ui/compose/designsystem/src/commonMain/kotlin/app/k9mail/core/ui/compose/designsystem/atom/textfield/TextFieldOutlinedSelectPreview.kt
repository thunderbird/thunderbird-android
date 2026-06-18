package app.k9mail.core.ui.compose.designsystem.atom.textfield

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import kotlinx.collections.immutable.persistentListOf

@Composable
@Preview(showBackground = true)
internal fun TextFieldOutlinedSelectPreview() {
    PreviewWithThemes {
        TextFieldOutlinedSelect(
            options = persistentListOf("Option 1", "Option 2", "Option 3"),
            selectedOption = "Option 1",
            onValueChange = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextFieldOutlinedSelectPreviewWithLabel() {
    PreviewWithThemes {
        TextFieldOutlinedSelect(
            options = persistentListOf("Option 1", "Option 2", "Option 3"),
            selectedOption = "Option 1",
            onValueChange = {},
            label = "Label",
        )
    }
}
