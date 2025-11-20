package net.thunderbird.core.ui.compose.designsystem.atom.icon.filled

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.core.ui.compose.designsystem.atom.icon.BadgeIcon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.badgeIcon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.badgeIconPath

@Suppress("MagicNumber")
internal val FilledUnreadMailBadge: ImageVector by lazy {
    badgeIcon(name = "FilledUnreadMailBadge") {
        badgeIconPath {
            moveTo(x = 5.99854f, y = 1.00024f)
            curveTo(x1 = 7.38181f, y1 = 1.00025f, x2 = 8.57321f, y2 = 1.48747f, x3 = 9.54248f, y3 = 2.45532f)
            curveTo(x1 = 10.512f, y1 = 3.42345f, x2 = 11.0005f, y2 = 4.6147f, x3 = 11.0005f, y3 = 5.99829f)
            curveTo(x1 = 11.0005f, y1 = 7.38164f, x2 = 10.5124f, y2 = 8.57294f, x3 = 9.54443f, y3 = 9.54224f)
            curveTo(x1 = 8.5764f, y1 = 10.5115f, x2 = 7.38583f, y2 = 11.0002f, x3 = 6.00244f, y3 = 11.0002f)
            curveTo(x1 = 4.61908f, y1 = 11.0002f, x2 = 3.4278f, y2 = 10.5121f, x3 = 2.4585f, y3 = 9.54419f)
            curveTo(x1 = 1.48911f, y1 = 8.57613f, x2 = 1.00055f, y2 = 7.38566f, x3 = 1.00049f, y3 = 6.0022f)
            curveTo(x1 = 1.00049f, y1 = 4.61899f, x2 = 1.48781f, y2 = 3.4275f, x3 = 2.45557f, y3 = 2.45825f)
            curveTo(x1 = 3.4237f, y1 = 1.48877f, x2 = 4.61494f, y2 = 1.00024f, x3 = 5.99854f, y3 = 1.00024f)
            close()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    BadgeIcon(imageVector = FilledUnreadMailBadge, contentDescription = null)
}
