package net.thunderbird.components.ui.bolt.atom.button

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton as Material3OutlinedButton
import androidx.compose.material3.Text as Material3Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.atom.icon.Icon
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.theme.MainTheme

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
    shape: ButtonShape = ButtonDefaults.outlinedShape(enabled),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
) {
    Material3OutlinedButton(
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
            )
        }
        Material3Text(
            text = text,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ButtonOutlinedPreview() {
    PreviewWithThemes {
        ButtonOutlined(
            text = "Button Outlined",
            onClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ButtonOutlinedDisabledPreview() {
    PreviewWithThemes {
        ButtonOutlined(
            text = "Button Outlined Disabled",
            onClick = {},
            enabled = false,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ButtonOutlinedMultiLinePreview() {
    PreviewWithThemes {
        ButtonOutlined(
            text = "First\nSecond line",
            onClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ButtonOutlinedIconPreview() {
    PreviewWithThemes {
        ButtonOutlined(
            text = "Button Outlined with Icon",
            icon = Icons.Outlined.Upload,
            onClick = {},
        )
    }
}
