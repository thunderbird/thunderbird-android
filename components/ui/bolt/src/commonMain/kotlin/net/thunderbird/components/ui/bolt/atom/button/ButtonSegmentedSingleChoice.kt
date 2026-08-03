package net.thunderbird.components.ui.bolt.atom.button

import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.atom.text.TextLabelLarge

/**
 * A segmented button group that allows the user to select a single option from a list of options.
 *
 * @param onClick The callback to be invoked when an option is clicked.
 * @param options The list of options to be displayed.
 * @param optionTitle A function that returns the title of an option.
 * @param modifier The [Modifier] to be applied to the segmented button group.
 * @param selectedOption The currently selected option. If null, no option is selected.
 */
@Composable
fun <T> ButtonSegmentedSingleChoice(
    onClick: (T) -> Unit,
    options: ImmutableList<T>,
    optionTitle: (T) -> String,
    modifier: Modifier = Modifier,
    selectedOption: T? = null,
) {
    if (options.isEmpty()) {
        return
    }

    SingleChoiceSegmentedButtonRow(
        modifier = modifier,
    ) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = options.size,
                ),
                onClick = {
                    onClick(option)
                },
                selected = option == selectedOption,
                label = {
                    TextLabelLarge(
                        text = optionTitle(option),
                    )
                },
            )
        }
    }
}

private val options = persistentListOf<String>(
    "Option 1",
    "Option 2",
    "Option 3",
)

@Composable
@Preview(showBackground = true)
internal fun ButtonSegmentedSingleChoicePreview() {
    PreviewWithThemes {
        ButtonSegmentedSingleChoice(
            modifier = Modifier,
            onClick = {},
            options = options,
            optionTitle = { it },
            selectedOption = null,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ButtonSegmentedSingleChoiceWithSelectionPreview() {
    PreviewWithThemes {
        ButtonSegmentedSingleChoice(
            modifier = Modifier,
            onClick = {},
            options = options,
            optionTitle = { it },
            selectedOption = options[1],
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ButtonSegmentedSingleChoiceEmptyPreview() {
    PreviewWithThemes {
        ButtonSegmentedSingleChoice(
            modifier = Modifier,
            onClick = {},
            options = persistentListOf<String>(),
            optionTitle = { it },
            selectedOption = null,
        )
    }
}
