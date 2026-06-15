package app.k9mail.core.ui.compose.designsystem.molecule.input

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import kotlinx.collections.immutable.persistentListOf

@Composable
@Preview(showBackground = true)
internal fun SelectInputPreview() {
    PreviewWithThemes {
        SelectInput(
            options = persistentListOf("Option 1", "Option 2", "Option 3"),
            selectedOption = "Option 1",
            onOptionChange = {},
        )
    }
}
