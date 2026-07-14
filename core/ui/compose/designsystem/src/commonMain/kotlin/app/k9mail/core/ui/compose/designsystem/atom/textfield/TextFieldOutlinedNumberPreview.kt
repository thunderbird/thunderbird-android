package app.k9mail.core.ui.compose.designsystem.atom.textfield

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes

@Composable
@Preview(showBackground = true)
internal fun TextFieldOutlinedNumberPreview() {
    PreviewWithThemes {
        TextFieldOutlinedNumber(
            value = 123L,
            onValueChange = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextFieldOutlinedNumberWithLabelPreview() {
    PreviewWithThemes {
        TextFieldOutlinedNumber(
            value = 123L,
            label = "Label",
            onValueChange = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextFieldOutlinedNumberDisabledPreview() {
    PreviewWithThemes {
        TextFieldOutlinedNumber(
            value = 123L,
            onValueChange = {},
            isEnabled = false,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextFieldOutlinedNumberErrorPreview() {
    PreviewWithThemes {
        TextFieldOutlinedNumber(
            value = 123L,
            onValueChange = {},
            hasError = true,
        )
    }
}
