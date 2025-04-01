package net.thunderbird.core.ui.compose.preference.ui.components.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.compose.preference.api.PreferenceSetting
import net.thunderbird.core.ui.compose.preference.ui.components.common.ColorView

@Composable
internal fun PreferenceDialogColorView(
    preference: PreferenceSetting.Color,
    onConfirmClick: (PreferenceSetting<*>) -> Unit,
    onDismissClick: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentColor = rememberSaveable { mutableIntStateOf(preference.value) }
    val gridState = rememberLazyGridState()

    PreferenceDialogLayout(
        title = preference.title(),
        icon = preference.icon(),
        onConfirmClick = { onConfirmClick(preference.copy(value = currentColor.intValue)) },
        onDismissClick = onDismissClick,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        preference.description()?.let {
            TextBodyMedium(text = it)

            Spacer(modifier = Modifier.height(MainTheme.spacings.double))
        }
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(minSize = MainTheme.sizes.iconAvatar),
            verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
            horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
        ) {
            items(preference.colors) { color ->
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
