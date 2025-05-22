package app.k9mail.core.ui.compose.designsystem.atom

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelLarge
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    onClick = {
                        onClick(option)
                    },
                    selected = option == selectedOption,
                )

                TextLabelLarge(
                    text = optionTitle(option),
                )
            }
        }
    }
}
