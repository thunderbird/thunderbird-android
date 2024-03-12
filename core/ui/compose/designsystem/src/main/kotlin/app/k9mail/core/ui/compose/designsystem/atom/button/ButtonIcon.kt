package app.k9mail.core.ui.compose.designsystem.atom.button

import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.theme.MainTheme
import androidx.compose.material.IconButton as MaterialIconButton

@Composable
fun ButtonIcon(
    onClick: () -> Unit,
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
) {
    MaterialIconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    ) {
        Icon(
            modifier = Modifier.size(MainTheme.sizes.icon),
            imageVector = imageVector,
            contentDescription = contentDescription,
        )
    }
}

@Preview
@Composable
internal fun ButtonIconPreview() {
    ButtonIcon(
        onClick = { },
        imageVector = Icons.Filled.user,
    )
}
