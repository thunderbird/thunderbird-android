package net.thunderbird.components.ui.bolt.molecule.input

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.thunderbird.components.ui.bolt.atom.textfield.TextFieldOutlinedNumber

@Suppress("LongParameterList")
@Composable
fun NumberInput(
    onValueChange: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    value: Long? = null,
    label: String? = null,
    isRequired: Boolean = false,
    errorMessage: String? = null,
    contentPadding: PaddingValues = inputContentPadding(),
) {
    InputLayout(
        modifier = modifier,
        contentPadding = contentPadding,
        errorMessage = errorMessage,
    ) {
        TextFieldOutlinedNumber(
            value = value,
            onValueChange = onValueChange,
            label = label,
            isRequired = isRequired,
            hasError = errorMessage != null,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
