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
        val error: ImageVector
            get() = MaterialIcons.Filled.Error

        val inbox: ImageVector
            get() = MaterialIcons.Filled.MoveToInbox

        val outbox: ImageVector
            get() = MaterialIcons.Filled.Outbox

        val security: ImageVector
            get() = MaterialIcons.Filled.Security

        val passwordVisibility: ImageVector
            get() = MaterialIcons.Filled.Visibility

        val passwordVisibilityOff: ImageVector
            get() = MaterialIcons.Filled.VisibilityOff

        val user: ImageVector
            get() = MaterialIcons.Filled.AccountCircle

        val check: ImageVector
            get() = MaterialIcons.Filled.CheckCircle

        val cancel: ImageVector
            get() = MaterialIcons.Filled.Cancel
    }

    object Outlined {
        val arrowBack: ImageVector
            get() = MaterialIcons.AutoMirrored.Outlined.ArrowBack

        val arrowDropDown: ImageVector
            get() = MaterialIcons.Outlined.ArrowDropDown

        val celebration: ImageVector
            get() = MaterialIcons.Outlined.Celebration

        val menu: ImageVector
            get() = MaterialIcons.Outlined.Menu

        val check: ImageVector
            get() = MaterialIcons.Outlined.Check

        val info: ImageVector
            get() = MaterialIcons.Outlined.Info

        val error: ImageVector
            get() = MaterialIcons.Outlined.ErrorOutline

        val expandMore: ImageVector
            get() = MaterialIcons.Outlined.ExpandMore

        val expandLess: ImageVector
            get() = MaterialIcons.Outlined.ExpandLess
    }
}
