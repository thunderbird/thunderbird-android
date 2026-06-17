package app.k9mail.core.ui.compose.designsystem.atom.textfield

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

fun sensitiveKeyboardOptions(usePrivateKeyboard: Boolean = true): KeyboardOptions {
    return if (usePrivateKeyboard) {
        KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false)
    } else {
        KeyboardOptions.Default
    }
}

fun nonLearningKeyboardOptions(usePrivateKeyboard: Boolean = true): KeyboardOptions {
    return if (usePrivateKeyboard) {
        KeyboardOptions(autoCorrectEnabled = false)
    } else {
        KeyboardOptions.Default
    }
}
