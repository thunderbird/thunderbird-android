package net.thunderbird.feature.account.settings.impl.ui.general

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.SettingValue.CompactSelectSingleOption.CompactOption
import net.thunderbird.core.ui.setting.SettingViewProvider
import net.thunderbird.feature.account.profile.AccountAvatar
import net.thunderbird.feature.account.settings.R
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.Event
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.SettingsBuilder
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.State

@Composable
internal fun GeneralSettingsContent(
    state: State,
    onEvent: (Event) -> Unit,
    provider: SettingViewProvider,
    builder: SettingsBuilder,
    modifier: Modifier = Modifier,
) {
    val settings = remember(state, builder) {
        builder.build(state = state)
    }

    provider.SettingView(
        title = stringResource(R.string.account_settings_general_title),
        subtitle = state.subtitle,
        settings = settings,
        onSettingValueChange = { setting ->
            when (setting) {
                is SettingValue.Text -> when (setting.id) {
                    GeneralSettingId.NAME -> onEvent(Event.OnNameChange(setting.value))
                }

                is SettingValue.Color -> if (setting.id == GeneralSettingId.COLOR) {
                    onEvent(Event.OnColorChange(setting.value))
                }

                is SettingValue.CompactSelectSingleOption<*> -> if (setting.id == GeneralSettingId.AVATAR) {
                    @Suppress("UNCHECKED_CAST")
                    val avatarOption = setting.value as CompactOption<AccountAvatar>
                    onEvent(Event.OnAvatarChange(avatarOption.value))
                }

                else -> Unit
            }
        },
        onBack = { onEvent(Event.OnBackPressed) },
        modifier = modifier,
    )
}
