package net.thunderbird.core.ui.compose.preference.ui.components.dialog

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
import net.thunderbird.core.ui.compose.preference.R

@Composable
internal fun PreferenceDialogLayout(
    title: String,
    icon: ImageVector?,
    onConfirmClick: () -> Unit,
    onDismissClick: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    AlertDialog(
        title = title,
        icon = icon,
        confirmText = stringResource(id = R.string.core_ui_preference_dialog_button_accept),
        onConfirmClick = onConfirmClick,
        dismissText = stringResource(id = R.string.core_ui_preference_dialog_button_cancel),
        onDismissClick = onDismissClick,
        onDismissRequest = onDismissRequest,
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
