package app.k9mail.core.ui.compose.designsystem.atom.button

import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import app.k9mail.core.ui.compose.theme2.MainTheme
import androidx.compose.material3.Icon as Material3Icon
import androidx.compose.material3.IconButton as Material3IconButton
import androidx.compose.material3.IconButtonColors as Material3IconButtonColors
import androidx.compose.material3.IconButtonDefaults as Material3IconButtonDefaults

@Composable
fun ButtonIcon(
    onClick: () -> Unit,
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    colors: ButtonIconColors = ButtonIconDefaults.buttonIconColors(),
) {
    Material3IconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = colors.toMaterial3Colors(),
    ) {
        Material3Icon(
            modifier = Modifier.size(MainTheme.sizes.icon),
            imageVector = imageVector,
            contentDescription = contentDescription,
        )
    }
}

object ButtonIconDefaults {
    private const val DISABLED_ICON_OPACITY = 0.38f

    @Composable
    fun buttonIconColors(): ButtonIconColors = Material3IconButtonDefaults.iconButtonColors().toButtonIconColors()

    /**
     * Creates a [ButtonIconColors] that represents the default colors used in a [ButtonIcon].
     *
     * @param containerColor the container color of this icon button when enabled.
     * @param contentColor the content color of this icon button when enabled.
     * @param disabledContainerColor the container color of this icon button when not enabled.
     * @param disabledContentColor the content color of this icon button when not enabled.
     */
    @Composable
    fun buttonIconColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = LocalContentColor.current,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = contentColor.copy(alpha = DISABLED_ICON_OPACITY),
    ): ButtonIconColors = Material3IconButtonDefaults.iconButtonColors(
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = disabledContentColor,
    ).toButtonIconColors()

    /**
     * Creates a [ButtonIconColors] that represents the default colors used in a [ButtonIcon].
     */
    @Composable
    fun buttonIconFilledColors(): ButtonIconColors =
        Material3IconButtonDefaults.filledIconButtonColors().toButtonIconColors()

    /**
     * Creates a [ButtonIconColors] that represents the default colors used in a [ButtonIcon].
     *
     * @param containerColor the container color of this icon button when enabled.
     * @param contentColor the content color of this icon button when enabled.
     * @param disabledContainerColor the container color of this icon button when not enabled.
     * @param disabledContentColor the content color of this icon button when not enabled.
     */
    @Composable
    fun buttonIconFilledColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = contentColorFor(containerColor),
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified,
    ): ButtonIconColors = Material3IconButtonDefaults.filledIconButtonColors(
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = disabledContentColor,
    ).toButtonIconColors()
}

/**
 * Represents the container and content colors used in an icon button in different states.
 *
 * @param containerColor the container color of this icon button when enabled.
 * @param contentColor the content color of this icon button when enabled.
 * @param disabledContainerColor the container color of this icon button when not enabled.
 * @param disabledContentColor the content color of this icon button when not enabled.
 * @constructor create an instance with arbitrary colors.
 */
data class ButtonIconColors(
    val containerColor: Color,
    val contentColor: Color,
    val disabledContainerColor: Color,
    val disabledContentColor: Color,
)

internal fun ButtonIconColors.toMaterial3Colors(): Material3IconButtonColors = Material3IconButtonColors(
    containerColor = containerColor,
    contentColor = contentColor,
    disabledContainerColor = disabledContainerColor,
    disabledContentColor = disabledContentColor,
)

internal fun Material3IconButtonColors.toButtonIconColors(): ButtonIconColors = ButtonIconColors(
    containerColor = containerColor,
    contentColor = contentColor,
    disabledContainerColor = disabledContainerColor,
    disabledContentColor = disabledContentColor,
)
