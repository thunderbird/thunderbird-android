package net.thunderbird.core.ui.compose.preference.ui.fake

import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.core.ui.compose.preference.api.PreferenceSetting
import net.thunderbird.core.ui.compose.preference.api.PreferenceSetting.SingleChoice.Choice

internal object FakePreferenceData {

    val textPreference = PreferenceSetting.Text(
        id = "text",
        icon = { Icons.Outlined.Delete },
        title = { "Title" },
        description = { "Description" },
        value = "Value",
    )

    val colorPreference = PreferenceSetting.Color(
        id = "color",
        icon = { Icons.Outlined.Delete },
        title = { "Title" },
        description = { "Description" },
        value = 0xFFFF0000.toInt(),
        colors = persistentListOf(
            0xFFFF0000.toInt(),
            0xFF00FF00.toInt(),
            0xFF0000FF.toInt(),
        ),
    )

    private val choices = persistentListOf<Choice>(
        Choice("1") { "Choice 1" },
        Choice("2") { "Choice 2" },
        Choice("3") { "Choice 3" },
    )

    val singleChoicePreference = PreferenceSetting.SingleChoice(
        id = "single_choice",
        title = { "Title" },
        description = { "Description" },
        value = choices[1],
        options = choices,
    )

    val switchPreference = PreferenceSetting.Switch(
        id = "switch",
        title = { "Title" },
        description = { "Description" },
        enabled = true,
        value = true,
    )

    val preferences = persistentListOf(
        textPreference,
        colorPreference,
        switchPreference,
        singleChoicePreference,
    )
}
