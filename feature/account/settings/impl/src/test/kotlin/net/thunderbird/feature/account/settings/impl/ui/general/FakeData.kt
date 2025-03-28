package net.thunderbird.feature.account.settings.impl.ui.general

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.core.ui.compose.preference.api.Preference
import net.thunderbird.core.ui.compose.preference.api.PreferenceSetting

internal object FakeData {

    val preferences: ImmutableList<Preference> = persistentListOf(
        PreferenceSetting.Text(
            id = "test_id",
            title = { "Title" },
            description = { "Description" },
            icon = { null },
            value = "Test",
        ),
    )
}
