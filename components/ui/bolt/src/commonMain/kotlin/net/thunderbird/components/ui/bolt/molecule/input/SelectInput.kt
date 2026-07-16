package net.thunderbird.components.ui.bolt.molecule.input

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.atom.textfield.TextFieldOutlinedSelect

@Composable
fun <T> SelectInput(
    options: ImmutableList<T>,
    selectedOption: T,
    onOptionChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    optionToStringTransformation: (T) -> String = { it.toString() },
    label: String? = null,
    contentPadding: PaddingValues = inputContentPadding(),
) {
    Column(
        modifier = Modifier
            .padding(contentPadding)
            .fillMaxWidth()
            .then(modifier),
    ) {
        TextFieldOutlinedSelect(
            options = options,
            selectedOption = selectedOption,
            onValueChange = onOptionChange,
            modifier = Modifier.fillMaxWidth(),
            optionToStringTransformation = optionToStringTransformation,
            label = label,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun SelectInputPreview() {
    PreviewWithThemes {
        SelectInput(
            options = persistentListOf("Option 1", "Option 2", "Option 3"),
            selectedOption = "Option 1",
            onOptionChange = {},
        )
    }
}
