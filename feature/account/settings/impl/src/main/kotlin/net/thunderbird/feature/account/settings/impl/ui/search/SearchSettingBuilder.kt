package net.thunderbird.feature.account.settings.impl.ui.search

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import net.thunderbird.core.common.resources.StringsResourceManager
import net.thunderbird.core.ui.setting.Setting
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.SettingValue.Select.SelectOption
import net.thunderbird.core.ui.setting.Settings
import net.thunderbird.feature.account.settings.R

class SearchSettingBuilder(
    private val resources: StringsResourceManager,
) : SearchSettingsContract.SettingsBuilder {
    override fun build(
        state: SearchSettingsContract.State,
        onEvent: (SearchSettingsContract.Event) -> Unit,
    ): Settings {
        val settings = mutableListOf<Setting>()
        settings += serverSearchLimit(state.serverSearchLimit)
        return settings.toImmutableList()
    }

    private val selectOptions = persistentListOf(
        SelectOption(10.toString()) {
            resources.stringResource(R.string.account_settings_remote_search_num_results_entries_10)
        },
        SelectOption(25.toString()) {
            resources.stringResource(R.string.account_settings_remote_search_num_results_entries_25)
        },
        SelectOption(50.toString()) {
            resources.stringResource(R.string.account_settings_remote_search_num_results_entries_50)
        },
        SelectOption(100.toString()) {
            resources.stringResource(R.string.account_settings_remote_search_num_results_entries_100)
        },
        SelectOption(250.toString()) {
            resources.stringResource(R.string.account_settings_remote_search_num_results_entries_250)
        },
        SelectOption(500.toString()) {
            resources.stringResource(R.string.account_settings_remote_search_num_results_entries_500)
        },
        SelectOption(1000.toString()) {
            resources.stringResource(R.string.account_settings_remote_search_num_results_entries_1000)
        },
        SelectOption(0.toString()) {
            resources.stringResource(R.string.account_settings_remote_search_num_results_entries_all)
        },
    )

    private fun serverSearchLimit(value: SelectOption): Setting = SettingValue.Select(
        id = SearchSettingId.SERVER_SEARCH_LIMIT,
        title = { resources.stringResource(R.string.account_settings_remote_search_num_label) },
        description = { null },
        icon = { null },
        displayValueAsSecondaryText = true,
        value = value,
        options = selectOptions,
    )
}

@Suppress("MagicNumber")
enum class ServerSearchLimit(val count: Int) {
    ALL(0),
    TEN(10),
    TWENTY_FIVE(25),
    FIFTY(50),
    HUNDRED(100),
    TWO_HUNDRED_FIFTY(250),
    FIVE_HUNDRED(500),
    THOUSAND(1000),
}
