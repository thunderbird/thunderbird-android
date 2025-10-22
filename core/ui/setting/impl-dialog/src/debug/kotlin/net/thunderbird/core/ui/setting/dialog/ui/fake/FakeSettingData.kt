package net.thunderbird.core.ui.setting.dialog.ui.fake

import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.SettingValue.CompactSelectSingleOption.CompactOption
import net.thunderbird.core.ui.setting.SettingValue.SelectSingleOption.Option

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

    private val compactOptions = persistentListOf(
        CompactOption("1") { "Choice 1" },
        CompactOption("2") { "Choice 2" },
        CompactOption("3") { "Choice 3" },
    )

    val compactSelectSingleOption = SettingValue.CompactSelectSingleOption(
        id = "compact_select_single_option",
        title = { "Title" },
        description = { "Description" },
        value = compactOptions[1],
        options = compactOptions,
    )

    private val options = persistentListOf(
        Option("1") { "Compact Choice 1" },
        Option("2") { "Compact Choice 2" },
        Option("3") { "Compact Choice 3" },
        Option("1") { "Compact Choice 4" },
        Option("2") { "Compact Choice 5" },
        Option("3") { "Compact Choice 6" },
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
