package net.thunderbird.core.ui.compose.designsystem.atom.icon.outlined

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.iconPath

@Suppress("MagicNumber")
internal val OutlinedBankIcon: ImageVector by lazy {
    icon(
        name = "OutlinedBankIcon",
        viewportWidth = 960.0f,
        viewportHeight = 960.0f,
    ) {
        iconPath {
            moveTo(x = 200.0f, y = 680.0f)
            lineToRelative(dx = 0.0f, dy = -280.0f)
            lineToRelative(dx = 80.0f, dy = 0.0f)
            lineToRelative(dx = 0.0f, dy = 280.0f)
            lineToRelative(dx = -80.0f, dy = 0.0f)
            close()
            moveToRelative(dx = 240.0f, dy = 0.0f)
            lineToRelative(dx = 0.0f, dy = -280.0f)
            lineToRelative(dx = 80.0f, dy = 0.0f)
            lineToRelative(dx = 0.0f, dy = 280.0f)
            lineToRelative(dx = -80.0f, dy = 0.0f)
            close()
            moveTo(x = 80.0f, y = 840.0f)
            lineToRelative(dx = 0.0f, dy = -80.0f)
            lineToRelative(dx = 800.0f, dy = 0.0f)
            lineToRelative(dx = 0.0f, dy = 80.0f)
            lineTo(x = 80.0f, y = 840.0f)
            close()
            moveToRelative(dx = 600.0f, dy = -160.0f)
            lineToRelative(dx = 0.0f, dy = -280.0f)
            lineToRelative(dx = 80.0f, dy = 0.0f)
            lineToRelative(dx = 0.0f, dy = 280.0f)
            lineToRelative(dx = -80.0f, dy = 0.0f)
            close()
            moveTo(x = 80.0f, y = 320.0f)
            lineToRelative(dx = 0.0f, dy = -80.0f)
            lineToRelative(dx = 400.0f, dy = -200.0f)
            lineToRelative(dx = 400.0f, dy = 200.0f)
            lineToRelative(dx = 0.0f, dy = 80.0f)
            lineTo(x = 80.0f, y = 320.0f)
            close()
            moveToRelative(dx = 178.0f, dy = -80.0f)
            lineToRelative(dx = 444.0f, dy = 0.0f)
            lineToRelative(dx = -444.0f, dy = 0.0f)
            close()
            moveToRelative(dx = 0.0f, dy = 0.0f)
            lineToRelative(dx = 444.0f, dy = 0.0f)
            lineTo(x = 480.0f, y = 130.0f)
            lineTo(x = 258.0f, y = 240.0f)
            close()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    Icon(imageVector = OutlinedBankIcon, contentDescription = null)
}
