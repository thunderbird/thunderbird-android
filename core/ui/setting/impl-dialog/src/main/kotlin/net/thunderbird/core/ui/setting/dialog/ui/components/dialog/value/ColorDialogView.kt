package net.thunderbird.core.ui.setting.dialog.ui.components.dialog.value

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalFlexBoxApi
import androidx.compose.foundation.layout.FlexBox
import androidx.compose.foundation.layout.FlexDirection
import androidx.compose.foundation.layout.FlexWrap
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.theme.MainTheme
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.dialog.ui.components.common.ColorView
import net.thunderbird.core.ui.setting.dialog.ui.components.dialog.SettingDialogLayout

@OptIn(ExperimentalFlexBoxApi::class)
@Composable
internal fun ColorDialogView(
    setting: SettingValue.Color,
    onConfirmClick: (SettingValue<*>) -> Unit,
    onDismissClick: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentColor = rememberSaveable { mutableIntStateOf(setting.value) }

    SettingDialogLayout(
        title = setting.title(),
        icon = setting.icon(),
        onConfirmClick = { onConfirmClick(setting.copy(value = currentColor.intValue)) },
        onDismissClick = onDismissClick,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        setting.description()?.let {
            TextBodyMedium(text = it)

            Spacer(modifier = Modifier.height(MainTheme.spacings.double))
        }

        val gap = MainTheme.spacings.default
        FlexBox(
            config = {
                direction(FlexDirection.Row)
                wrap(FlexWrap.Wrap)
                gap(gap)
            },
            modifier = Modifier.height(IntrinsicSize.Min),
        ) {
            setting.colors.forEach { color ->
                Box(
                    contentAlignment = Alignment.Center,
                ) {
                    ColorView(
                        color = color,
                        onClick = { newColor ->
                            currentColor.intValue = newColor
                        },
                        isSelected = color == currentColor.intValue,
                        modifier = Modifier.size(MainTheme.sizes.iconAvatar),
                    )
                }
            }
        }
    }
}
