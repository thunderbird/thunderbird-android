package net.thunderbird.feature.account.settings.impl.ui.general

import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.Settings

internal object FakeData {

    val settings: Settings = persistentListOf(
        SettingValue.Text(
            id = "test_id",
            title = { "Title" },
            description = { "Description" },
            icon = { null },
            value = "Test",
        ),
    )
}
