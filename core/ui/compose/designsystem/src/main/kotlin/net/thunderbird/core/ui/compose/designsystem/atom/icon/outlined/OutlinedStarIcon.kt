package net.thunderbird.core.ui.compose.designsystem.atom.icon.outlined

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.iconPath

@Suppress("MagicNumber")
internal val OutlinedStarIcon: ImageVector by lazy {
    icon(name = "OutlinedStarIcon") {
        iconPath {
            moveTo(x = 22.0f, y = 9.74f)
            lineTo(x = 14.81f, y = 9.12f)
            lineTo(x = 12.0f, y = 2.5f)
            lineTo(x = 9.19f, y = 9.13f)
            lineTo(x = 2.0f, y = 9.74f)
            lineTo(x = 7.46f, y = 14.47f)
            lineTo(x = 5.82f, y = 21.5f)
            lineTo(x = 12.0f, y = 17.77f)
            lineTo(x = 18.18f, y = 21.5f)
            lineTo(x = 16.55f, y = 14.47f)
            lineTo(x = 22.0f, y = 9.74f)
            close()
            moveTo(x = 12.0f, y = 15.9f)
            lineTo(x = 8.24f, y = 18.17f)
            lineTo(x = 9.24f, y = 13.89f)
            lineTo(x = 5.92f, y = 11.01f)
            lineTo(x = 10.3f, y = 10.63f)
            lineTo(x = 12.0f, y = 6.6f)
            lineTo(x = 13.71f, y = 10.64f)
            lineTo(x = 18.09f, y = 11.02f)
            lineTo(x = 14.77f, y = 13.9f)
            lineTo(x = 15.77f, y = 18.18f)
            lineTo(x = 12.0f, y = 15.9f)
            close()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    Icon(imageVector = OutlinedStarIcon, contentDescription = null)
}
