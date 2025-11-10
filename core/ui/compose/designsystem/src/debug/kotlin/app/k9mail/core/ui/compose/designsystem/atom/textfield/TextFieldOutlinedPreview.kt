package app.k9mail.core.ui.compose.designsystem.atom.textfield

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons

@Composable
@Preview(showBackground = true)
internal fun TextFieldOutlinedPreview() {
    PreviewWithThemes {
        TextFieldOutlined(
            value = "Input text",
            onValueChange = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextFieldOutlinedWithLabelPreview() {
    PreviewWithThemes {
        TextFieldOutlined(
            value = "Input text",
            onValueChange = {},
            label = "Label",
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextFieldOutlinedDisabledPreview() {
    PreviewWithThemes {
        TextFieldOutlined(
            value = "Input text",
            onValueChange = {},
            isEnabled = false,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextFieldOutlinedErrorPreview() {
    PreviewWithThemes {
        TextFieldOutlined(
            value = "Input text",
            onValueChange = {},
            hasError = true,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextFieldOutlinedRequiredPreview() {
    PreviewWithThemes {
        TextFieldOutlined(
            value = "",
            onValueChange = {},
            label = "Label",
            isRequired = true,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextFieldOutlinedWithTrailingIconPreview() {
    PreviewWithThemes {
        TextFieldOutlined(
            value = "",
            onValueChange = {},
            trailingIcon = { Icon(imageVector = Icons.Outlined.AccountCircle) },
            isRequired = true,
        )
    }
}
