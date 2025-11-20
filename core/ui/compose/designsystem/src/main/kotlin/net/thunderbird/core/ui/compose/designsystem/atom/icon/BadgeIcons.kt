package net.thunderbird.core.ui.compose.designsystem.atom.icon

import androidx.compose.ui.graphics.vector.ImageVector
import net.thunderbird.core.ui.compose.designsystem.atom.icon.filled.FilledNewMailBadge
import net.thunderbird.core.ui.compose.designsystem.atom.icon.filled.FilledUnreadMailBadge

/**
 * Collection of badge icons for the design system using a 12x12 base grid.
 *
 * They are organized by style: [Filled].
 *
 * Badges are defined as `val` properties. Direct assignment is used as the underlying icons
 * are already efficiently cached singletons.
 */
object BadgeIcons {

    /**
     * Badges with filled style.
     */
    object Filled {
        val NewMail: ImageVector = FilledNewMailBadge
        val UnreadMail: ImageVector = FilledUnreadMailBadge
    }
}
