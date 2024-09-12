package app.k9mail.core.ui.compose.designsystem.atom.icon

import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Outbox
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.ui.graphics.vector.ImageVector
import app.k9mail.core.ui.compose.designsystem.atom.icon.filled.Dot
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

        val ArrowBack: ImageVector
            get() = MaterialIcons.AutoMirrored.Outlined.ArrowBack

        val Check: ImageVector
            get() = MaterialIcons.Outlined.Check

        val ErrorOutline: ImageVector
            get() = MaterialIcons.Outlined.ErrorOutline

        val ExpandMore: ImageVector
            get() = MaterialIcons.Outlined.ExpandMore

        val ExpandLess: ImageVector
            get() = MaterialIcons.Outlined.ExpandLess

        val Inbox: ImageVector
            get() = MaterialIcons.Outlined.Inbox

        val Info: ImageVector
            get() = MaterialIcons.Outlined.Info

        val Menu: ImageVector
            get() = MaterialIcons.Outlined.Menu

        val Outbox: ImageVector
            get() = MaterialIcons.Filled.Outbox

        val Security: ImageVector
            get() = MaterialIcons.Outlined.Security

        val Visibility: ImageVector
            get() = MaterialIcons.Outlined.Visibility

        val VisibilityOff: ImageVector
            get() = MaterialIcons.Filled.VisibilityOff
    }
}
