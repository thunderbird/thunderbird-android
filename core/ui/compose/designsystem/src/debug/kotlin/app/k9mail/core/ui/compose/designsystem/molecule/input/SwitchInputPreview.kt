package app.k9mail.core.ui.compose.designsystem.molecule.input

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes

@Composable
@Preview(showBackground = true)
internal fun SwitchInputPreview() {
    PreviewWithThemes {
        SwitchInput(
            text = "SwitchInput",
            checked = false,
            onCheckedChange = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun SwitchInputWithErrorPreview() {
    PreviewWithThemes {
        SwitchInput(
            text = "SwitchInput",
            checked = false,
            onCheckedChange = {},
            errorMessage = "Error message",
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun SwitchInputCheckedPreview() {
    PreviewWithThemes {
        SwitchInput(
            text = "SwitchInput",
            checked = true,
            onCheckedChange = {},
        )
    }
}
