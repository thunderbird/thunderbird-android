package net.thunderbird.components.ui.bolt.atom.button

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import net.thunderbird.components.ui.bolt.atom.icon.Icon
import net.thunderbird.components.ui.bolt.theme.MainTheme
import androidx.compose.material3.Button as Material3Button
import androidx.compose.material3.Text as Material3Text

@Composable
fun ButtonFilled(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    colors: ButtonColors = ButtonDefaults.filledButtonColors(),
    shape: ButtonShape = ButtonDefaults.filledShape(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
) {
    Material3Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape.shape,
        colors = colors.toMaterial3Colors(),
        border = shape.borderStroke?.toMaterial3BorderStroke(),
        contentPadding = contentPadding,
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
