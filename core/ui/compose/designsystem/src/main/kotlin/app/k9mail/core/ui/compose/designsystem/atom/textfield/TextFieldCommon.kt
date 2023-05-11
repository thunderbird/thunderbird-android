package app.k9mail.core.ui.compose.designsystem.atom.textfield

import androidx.compose.material.Text
import androidx.compose.runtime.Composable

internal fun selectLabel(label: String?): @Composable (() -> Unit)? {
    return if (label != null) {
        {
            Text(text = label)
        }
    } else {
        null
    }
}
