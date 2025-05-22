package net.thunderbird.core.ui.compose.preference.ui.fake

import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.core.ui.compose.preference.api.PreferenceSetting
import net.thunderbird.core.ui.compose.preference.api.PreferenceSetting.SingleChoice.Choice
import net.thunderbird.core.ui.compose.preference.api.PreferenceSetting.SingleChoiceCompact.CompactChoice

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

    private val compactChoices = persistentListOf<CompactChoice>(
        CompactChoice("1") { "Compact Choice 1" },
        CompactChoice("2") { "Compact Choice 2" },
        CompactChoice("3") { "Compact Choice 3" },
        CompactChoice("1") { "Compact Choice 4" },
        CompactChoice("2") { "Compact Choice 5" },
        CompactChoice("3") { "Compact Choice 6" },
    )

    val singleChoiceCompactPreference = PreferenceSetting.SingleChoiceCompact(
        id = "single_choice_compact",
        title = { "Title" },
        icon = { Icons.Outlined.Info },
        description = { "Description" },
        value = compactChoices[1],
        options = compactChoices,
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
