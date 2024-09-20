package app.k9mail.core.ui.compose.designsystem.atom.icon

import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Outbox
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AllInbox
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Drafts
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.ui.graphics.vector.ImageVector
import app.k9mail.core.ui.compose.designsystem.atom.icon.filled.Dot
import app.k9mail.core.ui.compose.designsystem.atom.icon.outlined.FolderManaged
import androidx.compose.material.icons.Icons as MaterialIcons

// We're using getters so not all icons are loaded into memory as soon as one of the nested objects is accessed.
object Icons {
    object Filled {
        val Cancel: ImageVector
            get() = MaterialIcons.Filled.Cancel

        val CheckCircle: ImageVector
            get() = MaterialIcons.Filled.CheckCircle

        val Dot: ImageVector
            get() = MaterialIcons.Filled.Dot

        val Star: ImageVector
            get() = MaterialIcons.Filled.Star
    }

    object Outlined {
        val AccountCircle: ImageVector
            get() = MaterialIcons.Outlined.AccountCircle

        val AllInbox: ImageVector
            get() = MaterialIcons.Outlined.AllInbox

        val Archive: ImageVector
            get() = MaterialIcons.Outlined.Archive

        val ArrowBack: ImageVector
            get() = MaterialIcons.AutoMirrored.Outlined.ArrowBack

        val Check: ImageVector
            get() = MaterialIcons.Outlined.Check

        val ChevronLeft: ImageVector
            get() = MaterialIcons.Outlined.ChevronLeft

        val ChevronRight: ImageVector
            get() = MaterialIcons.Outlined.ChevronRight

        val Delete: ImageVector
            get() = MaterialIcons.Outlined.Delete

        val Drafts: ImageVector
            get() = MaterialIcons.Outlined.Drafts

        val ErrorOutline: ImageVector
            get() = MaterialIcons.Outlined.ErrorOutline

        val ExpandMore: ImageVector
            get() = MaterialIcons.Outlined.ExpandMore

        val ExpandLess: ImageVector
            get() = MaterialIcons.Outlined.ExpandLess

        val Folder: ImageVector
            get() = MaterialIcons.Outlined.Folder

        val Inbox: ImageVector
            get() = MaterialIcons.Outlined.Inbox

        val Info: ImageVector
            get() = MaterialIcons.Outlined.Info

        val FolderManaged: ImageVector
            get() = MaterialIcons.Outlined.FolderManaged

        val Menu: ImageVector
            get() = MaterialIcons.Outlined.Menu

        val Outbox: ImageVector
            get() = MaterialIcons.Filled.Outbox

        val Security: ImageVector
            get() = MaterialIcons.Outlined.Security

        val Send: ImageVector
            get() = MaterialIcons.AutoMirrored.Outlined.Send

        val Settings: ImageVector
            get() = MaterialIcons.Outlined.Settings

        val Report: ImageVector
            get() = MaterialIcons.Outlined.Report

        val Visibility: ImageVector
            get() = MaterialIcons.Outlined.Visibility

        val VisibilityOff: ImageVector
            get() = MaterialIcons.Filled.VisibilityOff
    }
}
