package app.k9mail.core.ui.compose.designsystem.atom.textfield

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes

@Composable
@Preview(showBackground = true)
internal fun TextFieldOutlinedEmailAddressPreview() {
    PreviewWithThemes {
        TextFieldOutlinedEmailAddress(
            value = "Input text",
            onValueChange = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextFieldOutlinedEmailAddressWithLabelPreview() {
    PreviewWithThemes {
        TextFieldOutlinedEmailAddress(
            value = "Input text",
            label = "Label",
            onValueChange = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextFieldOutlinedEmailDisabledPreview() {
    PreviewWithThemes {
        TextFieldOutlinedEmailAddress(
            value = "Input text",
            onValueChange = {},
            isEnabled = false,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextFieldOutlinedEmailErrorPreview() {
    PreviewWithThemes {
        TextFieldOutlinedEmailAddress(
            value = "Input text",
            onValueChange = {},
            hasError = true,
        )
    }
}
