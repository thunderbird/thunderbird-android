package app.k9mail.core.ui.compose.designsystem.atom.textfield

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import kotlinx.collections.immutable.ImmutableList
import androidx.compose.material3.DropdownMenu as Material3DropdownMenu
import androidx.compose.material3.DropdownMenuItem as Material3DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox as Material3ExposedDropdownMenuBox
import androidx.compose.material3.OutlinedTextField as Material3OutlinedTextField
import androidx.compose.material3.Text as Material3Text

// TODO replace Material3 DropdownMenu with Material3 ExposedDropdownMenu once it's size issue is fixed
// see: https://issuetracker.google.com/issues/205589613
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongParameterList", "LongMethod")
@Composable
fun <T> TextFieldOutlinedSelect(
    options: ImmutableList<T>,
    selectedOption: T,
    onValueChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    optionToStringTransformation: (T) -> String = { it.toString() },
    label: String? = null,
    isEnabled: Boolean = true,
    isReadOnly: Boolean = false,
    isRequired: Boolean = false,
    hasError: Boolean = false,
) {
    var isExpanded by remember {
        mutableStateOf(false)
    }

    val isReadOnlyOrDisabled = isReadOnly || !isEnabled

    Material3ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = {
            isExpanded = if (isReadOnlyOrDisabled) {
                false
            } else {
                isExpanded.not()
            }
        },
    ) {
        Material3OutlinedTextField(
            value = optionToStringTransformation(selectedOption),
            onValueChange = { },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
                .then(modifier),
            enabled = isEnabled,
            readOnly = true,
            label = selectLabel(label, isRequired),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
            isError = hasError,
            singleLine = true,
            interactionSource = remember {
                MutableInteractionSource()
            },
        )

        if (isReadOnlyOrDisabled.not()) {
            Material3DropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { isExpanded = false },
                modifier = Modifier.exposedDropdownSize(),
            ) {
                options.forEach { option ->
                    Material3DropdownMenuItem(
                        text = {
                            Material3Text(
                                text = transformOptionWithSelectionHighlight(
                                    option,
                                    optionToStringTransformation(option),
                                    selectedOption,
                                ),
                            )
                        },
                        onClick = {
                            onValueChange(option)
                            isExpanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
    }
}

private fun <T> transformOptionWithSelectionHighlight(
    option: T,
    optionString: String,
    selectedOption: T,
): AnnotatedString {
    return buildAnnotatedString {
        if (option == selectedOption) {
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append(optionString)
            }
        } else {
            append(optionString)
        }
    }
}
