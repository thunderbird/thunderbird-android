package net.thunderbird.ui.catalog.ui.page.organism.items.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonFilled
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodySmall
import app.k9mail.core.ui.compose.designsystem.organism.AlertDialog
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.ui.catalog.ui.page.common.list.defaultItem
import net.thunderbird.ui.catalog.ui.page.common.list.defaultItemPadding
import net.thunderbird.ui.catalog.ui.page.common.list.sectionHeaderItem
import net.thunderbird.ui.catalog.ui.page.common.list.sectionSubtitleItem

internal fun LazyGridScope.alertDialogs() {
    sectionHeaderItem("Alert dialogs")
    sectionSubtitleItem("Simple dialog")
    alertDialogItem(
        title = "Simple dialog",
        text = "This is a simple dialog",
    )
    sectionSubtitleItem("Dialog with icon")
    alertDialogItem(
        icon = Icons.Outlined.Info,
        title = "Dialog with icon",
        text = "This is a dialog with icon",
    )
    sectionSubtitleItem("Dialog with cancel")
    alertDialogItem(
        icon = Icons.Outlined.AccountCircle,
        title = "Dialog with cancel",
        text = "This is a dialog with cancel",
        hasCancel = true,
    )
    sectionSubtitleItem("Dialog with custom content")
    alertDialogItem(
        title = "Dialog with custom content",
        text = "This is a dialog with custom content",
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
        ) {
            TextBodyLarge("Large body")
            TextBodyMedium("Medium body")
            TextBodySmall("Small body")
        }
    }
}

private fun LazyGridScope.alertDialogItem(
    title: String,
    text: String,
    icon: ImageVector? = null,
    hasCancel: Boolean = false,
    content: @Composable (() -> Unit)? = null,
) = defaultItem {
    var showDialog by remember { mutableStateOf(false) }

    ButtonFilled(
        text = "Show dialog",
        onClick = { showDialog = true },
        modifier = Modifier.padding(defaultItemPadding()),
    )

    if (showDialog) {
        if (content != null) {
            AlertDialog(
                title = title,
                confirmText = "Accept",
                onConfirmClick = { showDialog = false },
                dismissText = if (hasCancel) "Cancel" else null,
                onDismissClick = { showDialog = false },
                onDismissRequest = { showDialog = false },
            ) {
                content()
            }
        } else {
            AlertDialog(
                icon = icon,
                title = title,
                text = text,
                confirmText = "Accept",
                onConfirmClick = { showDialog = false },
                dismissText = if (hasCancel) "Cancel" else null,
                onDismissClick = { showDialog = false },
                onDismissRequest = { showDialog = false },
            )
        }
    }
}
