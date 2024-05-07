package app.k9mail.core.ui.compose.designsystem.atom.icon

import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MoveToInbox
import androidx.compose.material.icons.filled.Outbox
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons as MaterialIcons

// We're using getters so not all icons are loaded into memory as soon as one of the nested objects is accessed.
object Icons {
    object Filled {
        val Error: ImageVector
            get() = MaterialIcons.Filled.Error

        val Inbox: ImageVector
            get() = MaterialIcons.Filled.MoveToInbox

        val Outbox: ImageVector
            get() = MaterialIcons.Filled.Outbox

        val Security: ImageVector
            get() = MaterialIcons.Filled.Security

        val PasswordVisibility: ImageVector
            get() = MaterialIcons.Filled.Visibility

        val PasswordVisibilityOff: ImageVector
            get() = MaterialIcons.Filled.VisibilityOff

        val User: ImageVector
            get() = MaterialIcons.Filled.AccountCircle

        val Check: ImageVector
            get() = MaterialIcons.Filled.CheckCircle

        val Cancel: ImageVector
            get() = MaterialIcons.Filled.Cancel
    }

    object Outlined {
        val ArrowBack: ImageVector
            get() = MaterialIcons.AutoMirrored.Outlined.ArrowBack

        val ArrowDropDown: ImageVector
            get() = MaterialIcons.Outlined.ArrowDropDown

        val Celebration: ImageVector
            get() = MaterialIcons.Outlined.Celebration

        val Menu: ImageVector
            get() = MaterialIcons.Outlined.Menu

        val Check: ImageVector
            get() = MaterialIcons.Outlined.Check

        val Info: ImageVector
            get() = MaterialIcons.Outlined.Info

        val Error: ImageVector
            get() = MaterialIcons.Outlined.ErrorOutline

        val ExpandMore: ImageVector
            get() = MaterialIcons.Outlined.ExpandMore

        val ExpandLess: ImageVector
            get() = MaterialIcons.Outlined.ExpandLess
    }
}
