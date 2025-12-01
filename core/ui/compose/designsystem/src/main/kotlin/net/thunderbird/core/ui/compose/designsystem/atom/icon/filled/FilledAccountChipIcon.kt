package net.thunderbird.core.ui.compose.designsystem.atom.icon.filled

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.iconPath

internal val FilledAccountChipIcon: ImageVector by lazy {
    icon(name = "FilledAccountChipIcon") {
        iconPath {
            iconPath {
            }
        }
    }

}

@Preview
@Composable
private fun Preview() {
    Icon(imageVector = FilledAccountChipIcon, contentDescription = null)
}
