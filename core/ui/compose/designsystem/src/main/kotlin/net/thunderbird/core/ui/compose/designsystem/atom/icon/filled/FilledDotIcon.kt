package net.thunderbird.core.ui.compose.designsystem.atom.icon.filled

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.iconPath

@Suppress("MagicNumber")
internal val FilledDotIcon: ImageVector by lazy {
    icon(name = "FilledDotIcon") {
        iconPath {
            moveTo(12.0f, 6.0f)
            curveToRelative(-3.31f, 0.0f, -6.0f, 2.69f, -6.0f, 6.0f)
            reflectiveCurveToRelative(2.69f, 6.0f, 6.0f, 6.0f)
            reflectiveCurveToRelative(6.0f, -2.69f, 6.0f, -6.0f)
            reflectiveCurveToRelative(-2.69f, -6.0f, -6.0f, -6.0f)
            close()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    Icon(imageVector = FilledDotIcon, contentDescription = null)
}
