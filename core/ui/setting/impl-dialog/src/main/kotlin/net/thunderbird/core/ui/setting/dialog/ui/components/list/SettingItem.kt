package net.thunderbird.core.ui.setting.dialog.ui.components.list

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.thunderbird.core.ui.setting.Setting
import net.thunderbird.core.ui.setting.SettingDecoration
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.dialog.ui.components.list.decoration.CustomItem
import net.thunderbird.core.ui.setting.dialog.ui.components.list.decoration.SectionDividerItem
import net.thunderbird.core.ui.setting.dialog.ui.components.list.decoration.SectionHeaderItem
import net.thunderbird.core.ui.setting.dialog.ui.components.list.value.ColorItem
import net.thunderbird.core.ui.setting.dialog.ui.components.list.value.SegmentedButtonItem
import net.thunderbird.core.ui.setting.dialog.ui.components.list.value.SelectItem
import net.thunderbird.core.ui.setting.dialog.ui.components.list.value.SwitchItem
import net.thunderbird.core.ui.setting.dialog.ui.components.list.value.TextItem

@Composable
internal fun SettingItem(
    setting: Setting,
    onClick: () -> Unit,
    onSettingValueChange: (SettingValue<*>) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (setting) {
        is SettingValue<*> -> {
            RenderSettingValue(
                setting = setting,
                onClick = onClick,
                onSettingValueChange = onSettingValueChange,
                modifier = modifier,
            )
        }

        is SettingDecoration -> {
            RenderSettingDecoration(
                setting = setting,
            )
        }
    }
}

@Composable
private fun RenderSettingValue(
    setting: SettingValue<*>,
    onClick: () -> Unit,
    onSettingValueChange: (SettingValue<*>) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (setting) {
        is SettingValue.Text -> {
            TextItem(
                setting = setting,
                onClick = onClick,
                modifier = modifier,
            )
        }

        is SettingValue.Color -> {
            ColorItem(
                setting = setting,
                onClick = onClick,
                modifier = modifier,
            )
        }

        is SettingValue.SegmentedButton<*> -> {
            SegmentedButtonItem(
                setting = setting,
                onSettingValueChange = onSettingValueChange,
                modifier = modifier,
            )
        }

        is SettingValue.Select -> {
            SelectItem(
                setting = setting,
                onClick = onClick,
                modifier = modifier,
            )
        }

        is SettingValue.Switch -> {
            SwitchItem(
                setting = setting,
                onSettingValueChange = onSettingValueChange,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun RenderSettingDecoration(
    setting: SettingDecoration,
    modifier: Modifier = Modifier,
) {
    when (setting) {
        is SettingDecoration.Custom -> {
            CustomItem(
                setting = setting,
                modifier = modifier,
            )
        }

        is SettingDecoration.SectionHeader -> {
            SectionHeaderItem(
                setting = setting,
                modifier = modifier,
            )
        }

        is SettingDecoration.SectionDivider -> {
            SectionDividerItem(
                modifier = modifier,
            )
        }
    }
}
