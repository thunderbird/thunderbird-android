package app.k9mail.core.ui.compose.designsystem.atom.button

import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelLarge
import kotlinx.collections.immutable.ImmutableList

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
