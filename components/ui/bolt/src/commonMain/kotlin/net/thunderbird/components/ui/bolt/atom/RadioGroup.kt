package net.thunderbird.components.ui.bolt.atom

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.atom.button.RadioButton
import net.thunderbird.components.ui.bolt.theme.BoltTheme

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

val choice = persistentListOf(
    Pair("1", "Native Android"),
    Pair("2", "Native iOS"),
    Pair("3", "KMM"),
    Pair("4", "Flutter"),
)

@Composable
@Preview(showBackground = true)
internal fun RadioGroupSelectedPreview() {
    PreviewWithThemes {
        var selectedOption by remember { mutableStateOf(choice[0]) }
        RadioGroup(
            onClick = { selectedOption = it },
            options = choice,
            optionTitle = { it.second },
            selectedOption = selectedOption,
            modifier = Modifier.padding(BoltTheme.spacings.default),
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun RadioGroupUnSelectedPreview() {
    PreviewWithThemes {
        RadioGroup(
            onClick = {},
            options = choice,
            optionTitle = { it.second },
            modifier = Modifier.padding(BoltTheme.spacings.default),
        )
    }
}
