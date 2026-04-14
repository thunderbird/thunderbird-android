package app.k9mail.core.ui.compose.designsystem.atom.button

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.theme2.MainTheme
import androidx.compose.material3.OutlinedButton as Material3OutlinedButton
import androidx.compose.material3.Text as Material3Text

/**
 * Outlined button component.
 *
 * @param text The text to display inside the button.
 * @param onClick The callback to be invoked when the button is clicked.
 * @param modifier The modifier to be applied to the button.
 * @param enabled Controls the enabled state of the button.
 * @param icon Optional icon to display alongside the text.
 * @param shape Optional shape of the button.
 */
@Composable
fun ButtonOutlined(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    shape: ButtonShape = ButtonDefaults.outlinedShape(),
) {
    Material3OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape.shape,
        colors = colors.toMaterial3Colors(),
        border = shape.borderStroke?.toMaterial3BorderStroke(),
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                modifier = Modifier
                    .alignByBaseline()
                    .padding(end = MainTheme.spacings.default),
                tint = colors.iconColor,
            )
        }
        Material3Text(
            text = text,
            textAlign = TextAlign.Center,
        )
    }
}
