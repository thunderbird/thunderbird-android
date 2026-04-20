package net.thunderbird.core.ui.compose.designsystem.atom.icon.outlined

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.iconPath

internal val OutlinedQrCode: ImageVector by lazy {
    icon(name = "OutlinedQrCode", viewportWidth = 18f, viewportHeight = 18f) {
        iconPath {
            moveTo(x = 2.25f, y = 8.25f)
            verticalLineToRelative(dy = -6.0f)
            horizontalLineToRelative(dx = 6.0f)
            verticalLineToRelative(dy = 6.0f)
            close()
            moveToRelative(dx = 1.5f, dy = -1.5f)
            horizontalLineToRelative(dx = 3.0f)
            verticalLineToRelative(dy = -3.0f)
            horizontalLineToRelative(dx = -3.0f)
            close()
            moveToRelative(dx = -1.5f, dy = 9.0f)
            verticalLineToRelative(dy = -6.0f)
            horizontalLineToRelative(dx = 6.0f)
            verticalLineToRelative(dy = 6.0f)
            close()
            moveToRelative(dx = 1.5f, dy = -1.5f)
            horizontalLineToRelative(dx = 3.0f)
            verticalLineToRelative(dy = -3.0f)
            horizontalLineToRelative(dx = -3.0f)
            close()
            moveToRelative(dx = 6.0f, dy = -6.0f)
            verticalLineToRelative(dy = -6.0f)
            horizontalLineToRelative(dx = 6.0f)
            verticalLineToRelative(dy = 6.0f)
            close()
            moveToRelative(dx = 1.5f, dy = -1.5f)
            horizontalLineToRelative(dx = 3.0f)
            verticalLineToRelative(dy = -3.0f)
            horizontalLineToRelative(dx = -3.0f)
            close()
            moveToRelative(dx = 3.0f, dy = 9.0f)
            verticalLineToRelative(dy = -1.5f)
            horizontalLineToRelative(dx = 1.5f)
            verticalLineToRelative(dy = 1.5f)
            close()
            moveToRelative(dx = -4.5f, dy = -4.5f)
            verticalLineToRelative(dy = -1.5f)
            horizontalLineToRelative(dx = 1.5f)
            verticalLineToRelative(dy = 1.5f)
            close()
            moveToRelative(dx = 1.5f, dy = 1.5f)
            verticalLineToRelative(dy = -1.5f)
            horizontalLineToRelative(dx = 1.5f)
            verticalLineToRelative(dy = 1.5f)
            close()
            moveToRelative(dx = -1.5f, dy = 1.5f)
            verticalLineToRelative(dy = -1.5f)
            horizontalLineToRelative(dx = 1.5f)
            verticalLineToRelative(dy = 1.5f)
            close()
            moveToRelative(dx = 1.5f, dy = 1.5f)
            verticalLineToRelative(dy = -1.5f)
            horizontalLineToRelative(dx = 1.5f)
            verticalLineToRelative(dy = 1.5f)
            close()
            moveToRelative(dx = 1.5f, dy = -1.5f)
            verticalLineToRelative(dy = -1.5f)
            horizontalLineToRelative(dx = 1.5f)
            verticalLineToRelative(dy = 1.5f)
            close()
            moveToRelative(dx = 0.0f, dy = -3.0f)
            verticalLineToRelative(dy = -1.5f)
            horizontalLineToRelative(dx = 1.5f)
            verticalLineToRelative(dy = 1.5f)
            close()
            moveToRelative(dx = 1.5f, dy = 1.5f)
            verticalLineToRelative(dy = -1.5f)
            horizontalLineToRelative(dx = 1.5f)
            verticalLineToRelative(dy = 1.5f)
            close()
        }
    }
}

@Preview(name = "QrCode", showBackground = true)
@Composable
private fun QrCodePreview() {
    Icon(imageVector = OutlinedQrCode)
}
