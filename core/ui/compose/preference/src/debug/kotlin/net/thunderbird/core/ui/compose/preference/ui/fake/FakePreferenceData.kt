package net.thunderbird.core.ui.compose.preference.ui.fake

import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.core.ui.compose.preference.api.PreferenceSetting

object FakePreferenceData {

    val textPreference = PreferenceSetting.Text(
        id = "text",
        icon = { Icons.Outlined.Delete },
        title = { "Title" },
        description = { "Description" },
        value = "Value",
    )

    val preferences = persistentListOf(
        textPreference,
    )
}
