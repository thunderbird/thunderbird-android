package net.thunderbird.core.ui.setting.dialog.ui.components.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.thunderbird.core.ui.setting.Setting
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.dialog.ui.components.dialog.value.ColorDialogView
import net.thunderbird.core.ui.setting.dialog.ui.components.dialog.value.SelectSingleOptionDialogView
import net.thunderbird.core.ui.setting.dialog.ui.components.dialog.value.TextDialogView

@Composable
internal fun SettingDialog(
    setting: Setting,
    onConfirmClick: (SettingValue<*>) -> Unit,
    onDismissClick: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    require(setting is SettingValue<*>) {
        "Unsupported setting type: ${setting::class.java.simpleName}"
    }

    when (setting) {
        is SettingValue.Text -> {
            TextDialogView(
                setting = setting,
                onConfirmClick = onConfirmClick,
                onDismissClick = onDismissClick,
                onDismissRequest = onDismissRequest,
                modifier = modifier,
            )
        }

        is SettingValue.Color -> {
            ColorDialogView(
                setting = setting,
                onConfirmClick = onConfirmClick,
                onDismissClick = onDismissClick,
                onDismissRequest = onDismissRequest,
                modifier = modifier,
            )
        }

        is SettingValue.SelectSingleOption -> {
            SelectSingleOptionDialogView(
                setting = setting,
                onConfirmClick = onConfirmClick,
                onDismissClick = onDismissClick,
                onDismissRequest = onDismissRequest,
                modifier = modifier,
            )
        }

        // No dialog needed
        is SettingValue.CompactSelectSingleOption, is SettingValue.Switch -> Unit
    }
}
