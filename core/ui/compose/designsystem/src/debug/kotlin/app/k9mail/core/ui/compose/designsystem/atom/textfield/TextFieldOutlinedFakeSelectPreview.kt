package app.k9mail.core.ui.compose.designsystem.atom.textfield

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes

@Composable
@Preview(showBackground = true)
internal fun TextFieldOutlinedFakeSelectPreview() {
    PreviewWithThemes {
        TextFieldOutlinedFakeSelect(
            text = "Current value",
            onClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextFieldOutlinedFakeSelectPreviewWithLabel() {
    PreviewWithThemes {
        TextFieldOutlinedFakeSelect(
            text = "Current value",
            onClick = {},
            label = "Label",
        )
    }
}
