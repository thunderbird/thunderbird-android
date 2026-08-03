package net.thunderbird.feature.account.settings.impl.ui.fetchingMail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.components.ui.bolt.atom.DropdownMenuBox
import net.thunderbird.components.ui.bolt.atom.button.ButtonIcon
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.organism.AlertDialog
import net.thunderbird.core.common.provider.AppNameProvider
import net.thunderbird.core.ui.setting.Setting
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.SettingViewProvider
import net.thunderbird.feature.account.settings.R
import net.thunderbird.feature.account.settings.impl.ui.fetchingMail.FetchingMailSettingsContract.Event
import net.thunderbird.feature.account.settings.impl.ui.fetchingMail.FetchingMailSettingsContract.SettingsBuilder
import net.thunderbird.feature.account.settings.impl.ui.fetchingMail.FetchingMailSettingsContract.State

@Suppress("LongMethod")
@Composable
internal fun FetchingMailSettingsContent(
    state: State,
    onEvent: (Event) -> Unit,
    onAccountRemove: () -> Unit,
    provider: SettingViewProvider,
    builder: SettingsBuilder,
    appNameProvider: AppNameProvider,
    modifier: Modifier = Modifier,
) {
    val settings = remember(state, builder, onEvent) {
        builder.buildCoreFetchingMailSettings(state = state, onEvent = onEvent)
    }

    var showDialog by remember { mutableStateOf(false) }

    provider.SettingView(
        title = stringResource(R.string.account_settings_fetching_mail),
        subtitle = state.subtitle,
        settings = settings,
        onSettingValueChange = { setting ->
            handleSettingChange(setting, onEvent)
        },
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

@Suppress("CyclomaticComplexMethod")
private fun handleSettingChange(
    setting: Setting,
    onEvent: (Event) -> Unit,
) {
    when (setting) {
        is SettingValue.Switch -> {
            when (setting.id) {
                FetchingMailSettingsId.SYNC_SERVER_DELETIONS -> onEvent(
                    Event.OnSyncServerDeletionsToggle(setting.value),
                )

                FetchingMailSettingsId.MARK_AS_READ_WHEN_DELETED -> onEvent(
                    Event.OnMarkAsReadWhenDeletedToggle(setting.value),
                )

                else -> Unit
            }
        }

        is SettingValue.Select -> {
            when (setting.id) {
                FetchingMailSettingsId.LOCAL_FOLDER_SIZE -> {
                    onEvent(Event.OnLocalFolderSizeChange(setting.value))
                }

                FetchingMailSettingsId.SYNC_MESSAGE_FROM -> {
                    onEvent(Event.OnSyncMessageFromChange(setting.value))
                }

                FetchingMailSettingsId.FOLDER_POLL_FREQUENCY -> {
                    onEvent(Event.OnFolderPollFrequencyChange(setting.value))
                }

                FetchingMailSettingsId.WHEN_I_DELETE_A_MESSAGE -> {
                    onEvent(Event.OnWhenIDeleteAMessageChange(setting.value))
                }

                FetchingMailSettingsId.ERASE_DELETED_MESSAGE_ON_SERVER -> {
                    onEvent(Event.OnEraseDeletedMessageOnServerChange(setting.value))
                }

                FetchingMailSettingsId.FETCH_MESSAGE_UP_TO -> {
                    onEvent(Event.OnFetchMessageUpToChange(setting.value))
                }

                else -> Unit
            }
        }

        is SettingValue.Text -> {
            when (setting.id) {
                FetchingMailSettingsId.IN_COMING_SERVER -> {
                    onEvent(Event.OnInComingServerClick)
                }

                else -> Unit
            }
        }

        else -> Unit
    }
}
