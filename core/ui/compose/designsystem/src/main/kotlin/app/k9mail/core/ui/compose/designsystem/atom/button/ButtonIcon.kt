package app.k9mail.core.ui.compose.designsystem.atom.button

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.k9mail.core.ui.compose.theme2.MainTheme
import androidx.compose.material3.Icon as Material3Icon
import androidx.compose.material3.IconButton as Material3IconButton

@Composable
fun ButtonIcon(
    onClick: () -> Unit,
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
) {
    Material3IconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    ) {
        Material3Icon(
            modifier = Modifier.size(MainTheme.sizes.icon),
            imageVector = imageVector,
            contentDescription = contentDescription,
        )
    }
}
