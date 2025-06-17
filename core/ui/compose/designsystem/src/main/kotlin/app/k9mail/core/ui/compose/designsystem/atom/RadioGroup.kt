package app.k9mail.core.ui.compose.designsystem.atom

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.button.RadioButton
import kotlinx.collections.immutable.ImmutableList

@Composable
fun <T> RadioGroup(
    onClick: (T) -> Unit,
    options: ImmutableList<T>,
    optionTitle: (T) -> String,
    modifier: Modifier = Modifier,
    selectedOption: T? = null,
) {
    if (options.isEmpty()) {
        return
    }

    Column(modifier = modifier) {
        options.forEach { option ->
            RadioButton(
                label = optionTitle(option),
                onClick = { onClick(option) },
                selected = option == selectedOption,
            )
        }
    }
}
