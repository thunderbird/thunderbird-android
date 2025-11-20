package net.thunderbird.feature.navigation.drawer.dropdown.ui.setting

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.k9mail.core.ui.compose.designsystem.organism.drawer.NavigationDrawerItem
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon

@Composable
internal fun SettingListItem(
    label: String,
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    NavigationDrawerItem(
        label = label,
        onClick = onClick,
        modifier = modifier,
        selected = false,
        icon = {
            Icon(
                imageVector = icon,
            )
        },
    )
}
