package net.thunderbird.components.ui.bolt.atom.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import net.thunderbird.components.ui.bolt.theme.MainTheme
import androidx.compose.material3.Text as Material3Text
import androidx.compose.material3.TextButton as Material3TextButton

@Composable
fun ButtonText(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    Material3TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = color ?: MainTheme.colors.primary,
        ).toMaterial3Colors(),
    ) {
        leadingIcon?.invoke()
        Material3Text(
            text = text,
            textAlign = TextAlign.Center,
        )
    }
}
