package net.thunderbird.feature.account.settings.impl.ui.readingMail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.designsystem.atom.DropdownMenuBox
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonIcon
import app.k9mail.core.ui.compose.designsystem.organism.AlertDialog
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.core.common.provider.AppNameProvider
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons
import net.thunderbird.core.ui.setting.Setting
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.SettingViewProvider
import net.thunderbird.feature.account.settings.R
import net.thunderbird.feature.account.settings.impl.ui.readingMail.ReadingMailSettingsContract.Event
import net.thunderbird.feature.account.settings.impl.ui.readingMail.ReadingMailSettingsContract.SettingsBuilder
import net.thunderbird.feature.account.settings.impl.ui.readingMail.ReadingMailSettingsContract.State

@Suppress("LongMethod")
@Composable
internal fun ReadingMailSettingsContent(
    state: State,
    onEvent: (Event) -> Unit,
    onAccountRemove: () -> Unit,
    provider: SettingViewProvider,
    builder: SettingsBuilder,
    appNameProvider: AppNameProvider,
    modifier: Modifier = Modifier,
) {
    val settings = remember(state, builder, onEvent) {
        builder.build(state = state, onEvent = onEvent)
    }

    var showDialog by remember { mutableStateOf(false) }

    provider.SettingView(
        title = stringResource(R.string.account_settings_reading_mail),
        subtitle = state.subtitle,
        settings = settings,
        onSettingValueChange = { setting -> handleSettingChange(setting, onEvent) },
        onBack = { onEvent(Event.OnBackPressed) },
        modifier = modifier,
        actions = {
            var expanded by remember { mutableStateOf(false) }

            DropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { shouldExpand ->
                    expanded = shouldExpand
                },
                options = persistentListOf(
                    stringResource(R.string.account_settings_remove_account_action),
                ),
                onItemSelected = {
                    showDialog = true
                    expanded = false
                },
            ) {
                ButtonIcon(
                    onClick = { expanded = true },
                    imageVector = Icons.Outlined.MoreVert,
                )
            }
        },
    )

    if (showDialog) {
        AlertDialog(
            title = stringResource(R.string.account_settings_account_delete_dlg_title),
            text = stringResource(
                R.string.account_settings_account_delete_dlg_instructions_fmt,
                state.subtitle.toString(),
                appNameProvider.appName,
            ),
            confirmText = stringResource(R.string.account_settings_okay_action),
            dismissText = stringResource(R.string.account_settings_cancel_action),
            onConfirmClick = {
                showDialog = false
                onAccountRemove()
            },
            onDismissClick = { showDialog = false },
            onDismissRequest = { showDialog = false },
        )
    }
}

private fun handleSettingChange(
    setting: Setting,
    onEvent: (Event) -> Unit,
) {
    when (setting) {
        is SettingValue.Switch -> onEvent(Event.OnIsMarkMessageAsReadOnViewToggle(setting.value))
        is SettingValue.Select -> onEvent(Event.OnShowPicturesChange(setting.value))
        else -> Unit
    }
}
