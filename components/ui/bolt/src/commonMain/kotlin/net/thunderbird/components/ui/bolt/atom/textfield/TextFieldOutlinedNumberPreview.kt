package net.thunderbird.components.ui.bolt.atom.textfield

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes

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
