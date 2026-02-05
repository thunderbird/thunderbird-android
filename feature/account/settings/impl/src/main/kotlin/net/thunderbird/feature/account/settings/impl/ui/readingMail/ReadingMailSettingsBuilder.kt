package net.thunderbird.feature.account.settings.impl.ui.readingMail

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import net.thunderbird.core.android.account.ShowPictures
import net.thunderbird.core.common.resources.StringsResourceManager
import net.thunderbird.core.ui.setting.Setting
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.SettingValue.Select.SelectOption
import net.thunderbird.core.ui.setting.Settings
import net.thunderbird.feature.account.settings.R

class ReadingMailSettingsBuilder(
    private val resources: StringsResourceManager,
) : ReadingMailSettingsContract.SettingsBuilder {
    override fun build(
        state: ReadingMailSettingsContract.State,
        onEvent: (ReadingMailSettingsContract.Event) -> Unit,
    ): Settings {
        val settings = mutableListOf<Setting>()
        settings += alwaysShowImage(value = state.showPictures)
        settings += markedAsReadOnView(value = state.isMarkMessageAsReadOnView)
        return settings.toImmutableList()
    }

    private val selectOptions = persistentListOf(
        SelectOption(ShowPictures.NEVER.name) {
            resources.stringResource(R.string.account_settings_show_pictures_never)
        },
        SelectOption(ShowPictures.ALWAYS.name) {
            resources.stringResource(R.string.account_settings_show_pictures_always)
        },
        SelectOption(ShowPictures.ONLY_FROM_CONTACTS.name) {
            resources.stringResource(R.string.account_settings_show_pictures_only_from_contacts)
        },
    )

    private fun alwaysShowImage(value: SelectOption): Setting = SettingValue.Select(
        id = ReadingMailSettingId.SHOW_PICTURES,
        title = { resources.stringResource(R.string.account_settings_show_pictures_label) },
        description = { null },
        icon = { null },
        displayValueAsSecondaryText = true,
        value = value,
        options = selectOptions,
    )

    private fun markedAsReadOnView(value: Boolean): Setting = SettingValue.Switch(
        id = ReadingMailSettingId.MARKED_AS_READ_ON_VIEW,
        title = { resources.stringResource(R.string.account_settings_mark_message_as_read_on_view_label) },
        description = { resources.stringResource(R.string.account_settings_mark_message_as_read_on_view_summary) },
        value = value,
    )
}
