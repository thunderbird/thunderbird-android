package net.thunderbird.core.ui.compose.designsystem.atom.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Outbox
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AllInbox
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Attachment
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Drafts
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.ui.graphics.vector.ImageVector
import net.thunderbird.core.ui.compose.designsystem.atom.icon.dualtone.DualToneWarningIcon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.filled.FilledDotIcon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.filled.FilledStarIcon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.outlined.OutlinedFolderManagedIcon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.outlined.OutlinedOpenInNewIcon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.outlined.OutlinedStarIcon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.outlined.OutlinedWarningIcon

/**
 * Collection of standard icons for the design system using a 24x24 base grid.
 *
 * They are organized by style: [Filled], [Outlined], and [DualTone].
 * It acts as a facade, allowing for a gradual transition from Material Icons to custom icons
 * without requiring changes in consumer code.
 *
 * Icons are defined as `val` properties. Direct assignment is used as the underlying Material and custom icons
 * are already efficiently cached singletons.
 */
object Icons {

    /**
     * Icons with dual tone style.
     */
    object DualTone {
        val Warning: ImageVector = DualToneWarningIcon
    }

    /**
     * Icons with filled style.
     */
    object Filled {
        val Cancel: ImageVector = Icons.Filled.Cancel
        val CheckCircle: ImageVector = Icons.Filled.CheckCircle
        val Dot: ImageVector = FilledDotIcon
        val Star: ImageVector = FilledStarIcon
    }

    /**
     * Icons with outlined style.
     */
    object Outlined {
        val AccountCircle: ImageVector = Icons.Outlined.AccountCircle
        val Add: ImageVector = Icons.Outlined.Add
        val AllInbox: ImageVector = Icons.Outlined.AllInbox
        val Archive: ImageVector = Icons.Outlined.Archive
        val Attachment: ImageVector = Icons.Outlined.Attachment
        val ArrowBack: ImageVector = Icons.AutoMirrored.Outlined.ArrowBack

        val Check: ImageVector = Icons.Outlined.Check
        val CheckCircle: ImageVector = Icons.Outlined.CheckCircle
        val ChevronLeft: ImageVector = Icons.Outlined.ChevronLeft
        val ChevronRight: ImageVector = Icons.Outlined.ChevronRight
        val Close: ImageVector = Icons.Outlined.Close

        val Delete: ImageVector = Icons.Outlined.Delete
        val Drafts: ImageVector = Icons.Outlined.Drafts

        val ErrorOutline: ImageVector = Icons.Outlined.ErrorOutline
        val ExpandMore: ImageVector = Icons.Outlined.ExpandMore
        val ExpandLess: ImageVector = Icons.Outlined.ExpandLess

        val Folder: ImageVector = Icons.Outlined.Folder
        val FolderManaged: ImageVector = OutlinedFolderManagedIcon

        val Inbox: ImageVector = Icons.Outlined.Inbox
        val Info: ImageVector = Icons.Outlined.Info

        val KeyboardArrowDown: ImageVector = Icons.Outlined.KeyboardArrowDown
        val KeyboardArrowUp: ImageVector = Icons.Outlined.KeyboardArrowUp

        val Menu: ImageVector = Icons.Outlined.Menu

        val OpenInNew: ImageVector = OutlinedOpenInNewIcon
        val Outbox: ImageVector = Icons.Filled.Outbox

        val Security: ImageVector = Icons.Outlined.Security
        val Send: ImageVector = Icons.AutoMirrored.Outlined.Send
        val Settings: ImageVector = Icons.Outlined.Settings
        val Star: ImageVector = OutlinedStarIcon
        val Sync: ImageVector = Icons.Outlined.Sync

        val Report: ImageVector = Icons.Outlined.Report

        val Visibility: ImageVector = Icons.Outlined.Visibility
        val VisibilityOff: ImageVector = Icons.Filled.VisibilityOff

        val Warning: ImageVector = OutlinedWarningIcon
    }
}
