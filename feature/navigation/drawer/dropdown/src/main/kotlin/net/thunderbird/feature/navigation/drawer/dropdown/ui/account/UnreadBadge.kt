package net.thunderbird.feature.navigation.drawer.dropdown.ui.account

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelSmall
import app.k9mail.core.ui.compose.theme2.ColorRoles
import net.thunderbird.feature.navigation.drawer.dropdown.ui.common.labelForCount

@Composable
internal fun UnreadBadge(
    unreadCount: Int,
    accountColorRoles: ColorRoles,
    modifier: Modifier = Modifier.Companion,
) {
    if (unreadCount > 0) {
        val resources = LocalResources.current

        Surface(
            color = accountColorRoles.accent,
            shape = CircleShape,
            modifier = modifier,
        ) {
            TextLabelSmall(
                text = labelForCount(
                    count = unreadCount,
                    resources = resources,
                ),
                color = accountColorRoles.onAccent,
                modifier = Modifier.Companion.padding(
                    horizontal = 3.dp,
                    vertical = 2.dp,
                ),
            )
        }
    }
}
