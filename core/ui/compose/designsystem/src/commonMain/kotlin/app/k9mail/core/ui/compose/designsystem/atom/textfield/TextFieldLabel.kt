package app.k9mail.core.ui.compose.designsystem.atom.textfield

import androidx.compose.runtime.Composable
import androidx.compose.material3.Text as Material3Text

private const val ASTERISK = "*"

@Composable
internal fun TextFieldLabel(
    label: String,
    isRequired: Boolean,
) {
    Material3Text(
        text = if (isRequired) {
            "$label$ASTERISK"
        } else {
            label
        },
    )
}
