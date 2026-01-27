package net.thunderbird.core.ui.compose.designsystem.atom.icon.filled

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.iconPath

@Suppress("MagicNumber")
internal val FilledStarIcon: ImageVector by lazy {
    icon(name = "FilledStarIcon") {
        iconPath {
            moveTo(x = 12.0f, y = 17.77f)
            lineTo(x = 18.18f, y = 21.5f)
            lineTo(x = 16.54f, y = 14.47f)
            lineTo(x = 22.0f, y = 9.74f)
            lineTo(x = 14.81f, y = 9.13f)
            lineTo(x = 12.0f, y = 2.5f)
            lineTo(x = 9.19f, y = 9.13f)
            lineTo(x = 2.0f, y = 9.74f)
            lineTo(x = 7.46f, y = 14.47f)
            lineTo(x = 5.82f, y = 21.5f)
            lineTo(x = 12.0f, y = 17.77f)
            close()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    Icon(imageVector = FilledStarIcon, contentDescription = null)
}
