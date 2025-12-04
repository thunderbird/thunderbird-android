package net.thunderbird.core.ui.setting.dialog.ui.components.dialog.value

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.dialog.ui.components.common.ColorView
import net.thunderbird.core.ui.setting.dialog.ui.components.dialog.SettingDialogLayout

@Composable
internal fun ColorDialogView(
    setting: SettingValue.Color,
    onConfirmClick: (SettingValue<*>) -> Unit,
    onDismissClick: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentColor = rememberSaveable { mutableIntStateOf(setting.value) }
    val gridState = rememberLazyGridState()

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

        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(minSize = MainTheme.sizes.iconAvatar),
            verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
            horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
        ) {
            items(setting.colors) { color ->
                Box(
                    modifier = Modifier.fillMaxSize(),
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
