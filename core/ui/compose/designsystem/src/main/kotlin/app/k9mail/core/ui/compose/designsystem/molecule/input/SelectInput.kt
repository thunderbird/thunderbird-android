package app.k9mail.core.ui.compose.designsystem.molecule.input

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.textfield.TextFieldOutlinedSelect
import kotlinx.collections.immutable.ImmutableList

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
