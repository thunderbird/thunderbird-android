package net.thunderbird.core.ui.setting.dialog.ui.components.dialog.value

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.RadioGroup
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.dialog.ui.components.dialog.SettingDialogLayout

@Composable
internal fun SelectSingleOptionDialogView(
    setting: SettingValue.SelectSingleOption,
    onConfirmClick: (SettingValue<*>) -> Unit,
    onDismissClick: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val options by remember { mutableStateOf(setting.options) }
    var selectedOption by remember { mutableStateOf(setting.value) }

    SettingDialogLayout(
        title = setting.title(),
        icon = setting.icon(),
        onConfirmClick = {
            onConfirmClick(setting.copy(value = selectedOption))
        },
        onDismissClick = onDismissClick,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        setting.description()?.let {
            TextBodyMedium(text = it)

            Spacer(modifier = Modifier.height(MainTheme.spacings.default))
        }

        RadioGroup(
            onClick = { selectedOption = it },
            options = options,
            optionTitle = { it.title() },
            selectedOption = selectedOption,
        )
    }
}
