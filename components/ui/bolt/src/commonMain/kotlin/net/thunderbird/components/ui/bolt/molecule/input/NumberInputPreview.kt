package net.thunderbird.components.ui.bolt.molecule.input

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes

@Composable
@Preview(showBackground = true)
internal fun NumberInputPreview() {
    PreviewWithThemes {
        NumberInput(
            onValueChange = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun NumberInputIsRequiredPreview() {
    PreviewWithThemes {
        NumberInput(
            onValueChange = {},
            label = "Text input is required",
            isRequired = true,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun NumberInputWithErrorPreview() {
    PreviewWithThemes {
        NumberInput(
            onValueChange = {},
            errorMessage = "Text input error",
        )
    }
}
