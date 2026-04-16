package app.k9mail.core.ui.compose.designsystem.atom.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Button as Material3Button
import androidx.compose.material3.Text as Material3Text

@Composable
fun ButtonFilled(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
) {
    Material3Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    ) {
        leadingIcon?.invoke()
        Material3Text(
            text = text,
            textAlign = TextAlign.Center,
        )
    }
}
