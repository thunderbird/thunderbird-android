package net.thunderbird.feature.account.settings.impl.ui.general

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import net.thunderbird.core.ui.setting.Setting
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.SettingValue.SegmentedButton.SegmentedButtonOption
import net.thunderbird.core.ui.setting.SettingViewProvider
import net.thunderbird.feature.account.avatar.Avatar
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
        onSettingValueChange = { setting -> handleSettingChange(setting, onEvent) },
        onBack = { onEvent(Event.OnBackPressed) },
        modifier = modifier,
    )
}

private fun handleSettingChange(
    setting: Setting,
    onEvent: (Event) -> Unit,
) {
    when (setting) {
        is SettingValue.Text -> handleTextChange(setting, onEvent)
        is SettingValue.Color -> handleColorChange(setting, onEvent)
        is SettingValue.SegmentedButton<*> -> handleSegmentedChange(setting, onEvent)
        is SettingValue.IconList -> handleIconListChange(setting, onEvent)
        else -> Unit
    }
}

private fun handleTextChange(setting: SettingValue.Text, onEvent: (Event) -> Unit) {
    when (setting.id) {
        GeneralSettingId.NAME -> onEvent(Event.OnNameChange(setting.value))
        GeneralSettingId.AVATAR_MONOGRAM -> onEvent(Event.OnAvatarChange(Avatar.Monogram(setting.value)))
        else -> Unit
    }
}

private fun handleColorChange(setting: SettingValue.Color, onEvent: (Event) -> Unit) {
    if (setting.id == GeneralSettingId.COLOR) {
        onEvent(Event.OnColorChange(setting.value))
    }
}

private fun handleSegmentedChange(setting: SettingValue.SegmentedButton<*>, onEvent: (Event) -> Unit) {
    when (setting.id) {
        GeneralSettingId.AVATAR_OPTIONS -> {
            @Suppress("UNCHECKED_CAST")
            val avatarOption = setting.value as SegmentedButtonOption<Avatar>
            onEvent(Event.OnAvatarChange(avatarOption.value))
        }
        GeneralSettingId.AVATAR_ICON -> {
            @Suppress("UNCHECKED_CAST")
            val iconOption = setting.value as SegmentedButtonOption<Avatar>
            onEvent(Event.OnAvatarChange(iconOption.value))
        }
        else -> Unit
    }
}

private fun handleIconListChange(setting: SettingValue.IconList, onEvent: (Event) -> Unit) {
    when (setting.id) {
        GeneralSettingId.AVATAR_ICON -> {
            val iconOption = setting.icons.firstOrNull { it == setting.value }
            if (iconOption != null) {
                onEvent(Event.OnAvatarChange(Avatar.Icon(iconOption.id)))
            }
        }
        else -> Unit
    }
}
