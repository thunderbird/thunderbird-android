package app.k9mail.core.ui.compose.designsystem.atom.textfield

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.TextFieldValue

private val LINE_BREAK = "[\\r\\n]".toRegex()

internal fun stripLineBreaks(onValueChange: (String) -> Unit): (String) -> Unit = { value ->
    onValueChange(value.replace(LINE_BREAK, replacement = ""))
}

internal fun stripTextFieldValueLineBreaks(onValueChange: (TextFieldValue) -> Unit): (TextFieldValue) -> Unit {
    return { value ->
        onValueChange(value.copy(text = value.text.replace(LINE_BREAK, replacement = "")))
    }
}

internal fun selectLabel(
    label: String?,
    isRequired: Boolean,
): @Composable (() -> Unit)? {
    return if (label != null || isRequired) {
        {
            TextFieldLabel(label.orEmpty(), isRequired)
        }
    } else {
        null
    }
}
