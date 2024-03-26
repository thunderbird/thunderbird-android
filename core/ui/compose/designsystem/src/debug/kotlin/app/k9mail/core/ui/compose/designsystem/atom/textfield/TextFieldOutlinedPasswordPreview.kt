package app.k9mail.core.ui.compose.designsystem.atom.textfield

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes

@Composable
@Preview(showBackground = true)
internal fun TextFieldOutlinedPasswordPreview() {
    PreviewWithThemes {
        TextFieldOutlinedPassword(
            value = "Input text",
            onValueChange = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextFieldOutlinedPasswordWithLabelPreview() {
    PreviewWithThemes {
        TextFieldOutlinedPassword(
            value = "Input text",
            label = "Label",
            onValueChange = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextFieldOutlinedPasswordDisabledPreview() {
    PreviewWithThemes {
        TextFieldOutlinedPassword(
            value = "Input text",
            onValueChange = {},
            isEnabled = false,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextFieldOutlinedPasswordErrorPreview() {
    PreviewWithThemes {
        TextFieldOutlinedPassword(
            value = "Input text",
            onValueChange = {},
            hasError = true,
        )
    }
}
