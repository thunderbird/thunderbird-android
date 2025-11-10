package net.thunderbird.core.ui.setting.dialog.ui.components.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.designsystem.organism.AlertDialog
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.setting.dialog.R

@Composable
internal fun SettingDialogLayout(
    title: String,
    icon: ImageVector?,
    onConfirmClick: () -> Unit,
    onDismissClick: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    confirmButtonEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    AlertDialog(
        title = title,
        icon = icon,
        confirmText = stringResource(id = R.string.core_ui_setting_dialog_button_accept),
        onConfirmClick = onConfirmClick,
        dismissText = stringResource(id = R.string.core_ui_setting_dialog_button_cancel),
        onDismissClick = onDismissClick,
        onDismissRequest = onDismissRequest,
        confirmButtonEnabled = confirmButtonEnabled,
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.half),
            modifier = Modifier.fillMaxWidth(),
        ) {
            content()
        }
    }
}
