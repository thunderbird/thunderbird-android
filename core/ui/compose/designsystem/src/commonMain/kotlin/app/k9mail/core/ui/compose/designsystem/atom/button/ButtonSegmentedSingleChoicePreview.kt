package app.k9mail.core.ui.compose.designsystem.atom.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import kotlinx.collections.immutable.persistentListOf

private val options = persistentListOf<String>(
    "Option 1",
    "Option 2",
    "Option 3",
)

@Composable
@Preview(showBackground = true)
internal fun ButtonSegmentedSingleChoicePreview() {
    PreviewWithThemes {
        ButtonSegmentedSingleChoice(
            modifier = Modifier,
            onClick = {},
            options = options,
            optionTitle = { it },
            selectedOption = null,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ButtonSegmentedSingleChoiceWithSelectionPreview() {
    PreviewWithThemes {
        ButtonSegmentedSingleChoice(
            modifier = Modifier,
            onClick = {},
            options = options,
            optionTitle = { it },
            selectedOption = options[1],
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ButtonSegmentedSingleChoiceEmptyPreview() {
    PreviewWithThemes {
        ButtonSegmentedSingleChoice(
            modifier = Modifier,
            onClick = {},
            options = persistentListOf<String>(),
            optionTitle = { it },
            selectedOption = null,
        )
    }
}
