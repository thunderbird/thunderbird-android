package net.thunderbird.core.ui.setting.dialog.ui.fake

import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.SettingValue.SelectSingleOption.Option
import net.thunderbird.core.ui.setting.SettingValue.SegmentedButton.SegmentedButtonOption

internal object FakeSettingData {

    val text = SettingValue.Text(
        id = "text",
        icon = { Icons.Outlined.Delete },
        title = { "Title" },
        description = { "Description" },
        value = "Value",
    )

    val color = SettingValue.Color(
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

    private val segmentedButtonOptions = persistentListOf(
        SegmentedButtonOption(id = "1", title = { "Option 1" }, value = "1"),
        SegmentedButtonOption(id = "2", title = { "Option 2" }, value = "2"),
        SegmentedButtonOption(id = "3", title = { "Option 3" }, value = "3"),
    )

    val segmentedButton = SettingValue.SegmentedButton(
        id = "compact_select_single_option",
        title = { "Title" },
        description = { "Description" },
        value = segmentedButtonOptions[1],
        options = segmentedButtonOptions,
    )

    private val options = persistentListOf(
        Option("1") { "Option 1" },
        Option("2") { "Option 2" },
        Option("3") { "Option 3" },
        Option("4") { "Option 4" },
        Option("5") { "Option 5" },
        Option("6") { "Option 6" },
    )

    val selectSingleOption = SettingValue.SelectSingleOption(
        id = "select_single_option",
        title = { "Title" },
        icon = { Icons.Outlined.Info },
        description = { "Description" },
        value = options[1],
        options = options,
    )

    val switch = SettingValue.Switch(
        id = "switch",
        title = { "Title" },
        description = { "Description" },
        value = true,
    )

    val settings = persistentListOf(
        text,
        color,
        compactSelectSingleOption,
        selectSingleOption,
        switch,
    )
}
