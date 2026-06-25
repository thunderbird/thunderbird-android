package net.thunderbird.core.ui.compose.designsystem.atom.icon.outlined

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.iconPath

@Suppress("MagicNumber")
internal val OutlinedSchoolIcon: ImageVector by lazy {
    icon(
        name = "OutlinedSchoolIcon",
        viewportWidth = 960.0f,
        viewportHeight = 960.0f,
    ) {
        iconPath {
            moveTo(x = 480.0f, y = 840.0f)
            lineTo(x = 200.0f, y = 688.0f)
            lineToRelative(dx = 0.0f, dy = -240.0f)
            lineTo(x = 40.0f, y = 360.0f)
            lineToRelative(dx = 440.0f, dy = -240.0f)
            lineToRelative(dx = 440.0f, dy = 240.0f)
            lineToRelative(dx = 0.0f, dy = 320.0f)
            lineToRelative(dx = -80.0f, dy = 0.0f)
            lineToRelative(dx = 0.0f, dy = -276.0f)
            lineToRelative(dx = -80.0f, dy = 44.0f)
            lineToRelative(dx = 0.0f, dy = 240.0f)
            lineTo(x = 480.0f, y = 840.0f)
            close()
            moveToRelative(dx = 0.0f, dy = -332.0f)
            lineToRelative(dx = 274.0f, dy = -148.0f)
            lineToRelative(dx = -274.0f, dy = -148.0f)
            lineToRelative(dx = -274.0f, dy = 148.0f)
            lineToRelative(dx = 274.0f, dy = 148.0f)
            close()
            moveToRelative(dx = 0.0f, dy = 241.0f)
            lineToRelative(dx = 200.0f, dy = -108.0f)
            lineToRelative(dx = 0.0f, dy = -151.0f)
            lineTo(x = 480.0f, y = 600.0f)
            lineTo(x = 280.0f, y = 490.0f)
            lineToRelative(dx = 0.0f, dy = 151.0f)
            lineToRelative(dx = 200.0f, dy = 108.0f)
            close()
            moveToRelative(dx = 0.0f, dy = -241.0f)
            close()
            moveToRelative(dx = 0.0f, dy = 90.0f)
            close()
            moveToRelative(dx = 0.0f, dy = 0.0f)
            close()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    Icon(imageVector = OutlinedSchoolIcon, contentDescription = null)
}
