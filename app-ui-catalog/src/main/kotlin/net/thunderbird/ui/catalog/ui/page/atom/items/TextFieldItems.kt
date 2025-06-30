package net.thunderbird.ui.catalog.ui.page.atom.items

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icon
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.designsystem.atom.textfield.TextFieldOutlined
import app.k9mail.core.ui.compose.designsystem.atom.textfield.TextFieldOutlinedEmailAddress
import app.k9mail.core.ui.compose.designsystem.atom.textfield.TextFieldOutlinedNumber
import app.k9mail.core.ui.compose.designsystem.atom.textfield.TextFieldOutlinedPassword
import app.k9mail.core.ui.compose.designsystem.atom.textfield.TextFieldOutlinedSelect
import app.k9mail.core.ui.compose.designsystem.molecule.input.CheckboxInput
import app.k9mail.core.ui.compose.theme2.MainTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.ui.catalog.ui.page.common.helper.WithRememberedState
import net.thunderbird.ui.catalog.ui.page.common.list.defaultItemPadding
import net.thunderbird.ui.catalog.ui.page.common.list.fullSpanItem
import net.thunderbird.ui.catalog.ui.page.common.list.sectionHeaderItem
import net.thunderbird.ui.catalog.ui.page.common.list.sectionSubtitleItem

fun LazyGridScope.textFieldItems() {
    sectionHeaderItem(text = "Text field - Outlined")
    sectionSubtitleItem(text = "Default")
    textFieldOutlinedItems()
    sectionSubtitleItem(text = "Password")
    passwordTextFieldOutlinedItems()
    sectionSubtitleItem(text = "Email address")
    emailTextFieldOutlinedItems()
    sectionSubtitleItem(text = "Number")
    numberTextFieldOutlinedItems()
    sectionSubtitleItem(text = "Selection")
    selectionTextFieldOutlinedItems()
}

@Stable
data class TextFieldState<T>(
    val input: T,
    val label: String = "Label",
    val showLabel: Boolean = false,
    val showTrailingIcon: Boolean = false,
    val isDisabled: Boolean = false,
    val isReadOnly: Boolean = false,
    val isRequired: Boolean = false,
    val hasError: Boolean = false,
    val isSingleLine: Boolean = false,
)

@Suppress("LongMethod")
@Composable
fun <T> TextFieldDemo(
    initialState: TextFieldState<T>,
    modifier: Modifier = Modifier,
    hasTrailingIcon: Boolean = false,
    hasSingleLine: Boolean = false,
    content: @Composable (state: MutableState<TextFieldState<T>>) -> Unit,
) {
    WithRememberedState(input = initialState) { state ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(defaultItemPadding())
                .then(modifier),
        ) {
            key(state.value.showLabel, state.value.isRequired) {
                content(state)
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = MainTheme.spacings.double,
                            start = MainTheme.spacings.default,
                        ),
                ) {
                    TextTitleMedium(text = "Configuration:")
                }

                CheckboxInput(
                    text = "Show Label",
                    checked = state.value.showLabel,
                    onCheckedChange = { state.value = state.value.copy(showLabel = it) },
                    contentPadding = defaultPadding,
                )

                if (hasTrailingIcon) {
                    CheckboxInput(
                        text = "Show Trailing Icon",
                        checked = state.value.showTrailingIcon,
                        onCheckedChange = { state.value = state.value.copy(showTrailingIcon = it) },
                        contentPadding = defaultPadding,
                    )
                }

                CheckboxInput(
                    text = "Is required",
                    checked = state.value.isRequired,
                    onCheckedChange = { state.value = state.value.copy(isRequired = it) },
                    contentPadding = defaultPadding,
                )

                CheckboxInput(
                    text = "Is read-only",
                    checked = state.value.isReadOnly,
                    onCheckedChange = { state.value = state.value.copy(isReadOnly = it) },
                    contentPadding = defaultPadding,
                )

                CheckboxInput(
                    text = "Is disabled",
                    checked = state.value.isDisabled,
                    onCheckedChange = { state.value = state.value.copy(isDisabled = it) },
                    contentPadding = defaultPadding,
                )

                CheckboxInput(
                    text = "Has Error",
                    checked = state.value.hasError,
                    onCheckedChange = { state.value = state.value.copy(hasError = it) },
                    contentPadding = defaultPadding,
                )

                if (hasSingleLine) {
                    CheckboxInput(
                        text = "Single line",
                        checked = state.value.isSingleLine,
                        onCheckedChange = { state.value = state.value.copy(isSingleLine = it) },
                        contentPadding = defaultPadding,
                    )
                }
            }
        }
    }
}

private val defaultPadding = PaddingValues(0.dp)

private fun LazyGridScope.textFieldOutlinedItems() {
    fullSpanItem {
        TextFieldDemo(
            hasTrailingIcon = true,
            hasSingleLine = true,
            initialState = TextFieldState(input = ""),
        ) { state ->
            TextFieldOutlined(
                value = state.value.input,
                label = if (state.value.showLabel) state.value.label else null,
                onValueChange = { state.value = state.value.copy(input = it) },
                trailingIcon = {
                    if (state.value.showTrailingIcon) {
                        Icon(imageVector = Icons.Outlined.AccountCircle)
                    }
                },
                isEnabled = !state.value.isDisabled,
                isReadOnly = state.value.isReadOnly,
                isRequired = state.value.isRequired,
                isSingleLine = state.value.isSingleLine,
                hasError = state.value.hasError,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun LazyGridScope.passwordTextFieldOutlinedItems() {
    fullSpanItem {
        TextFieldDemo(
            initialState = TextFieldState(
                input = "",
                label = "Password",
            ),
        ) { state ->
            TextFieldOutlinedPassword(
                value = state.value.input,
                label = if (state.value.showLabel) state.value.label else null,
                onValueChange = { state.value = state.value.copy(input = it) },
                isEnabled = !state.value.isDisabled,
                isReadOnly = state.value.isReadOnly,
                isRequired = state.value.isRequired,
                hasError = state.value.hasError,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun LazyGridScope.emailTextFieldOutlinedItems() {
    fullSpanItem {
        TextFieldDemo(
            initialState = TextFieldState(
                input = "",
                label = "Email Address",
            ),
        ) { state ->
            TextFieldOutlinedEmailAddress(
                value = state.value.input,
                label = if (state.value.showLabel) state.value.label else null,
                onValueChange = { state.value = state.value.copy(input = it) },
                isEnabled = !state.value.isDisabled,
                isReadOnly = state.value.isReadOnly,
                isRequired = state.value.isRequired,
                hasError = state.value.hasError,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun LazyGridScope.numberTextFieldOutlinedItems() {
    fullSpanItem {
        TextFieldDemo(
            initialState = TextFieldState<Long?>(
                input = 123L,
                label = "Number",
            ),
        ) { state ->
            TextFieldOutlinedNumber(
                value = state.value.input,
                label = if (state.value.showLabel) state.value.label else null,
                onValueChange = { state.value = state.value.copy(input = it) },
                isEnabled = !state.value.isDisabled,
                isReadOnly = state.value.isReadOnly,
                isRequired = state.value.isRequired,
                hasError = state.value.hasError,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private data class TextFieldSelectState(
    val options: ImmutableList<String> = persistentListOf("Option 1", "Option 2", "Option 3"),
    val selectedOption: String = options.first(),
)

private fun LazyGridScope.selectionTextFieldOutlinedItems() {
    fullSpanItem {
        TextFieldDemo(
            initialState = TextFieldState(
                input = TextFieldSelectState(),
                label = "Select",
            ),
        ) { state ->
            key(
                state.value.input.selectedOption,
            ) {
                TextFieldOutlinedSelect(
                    options = state.value.input.options,
                    label = if (state.value.showLabel) state.value.label else null,
                    onValueChange = {
                        state.value = state.value.copy(input = state.value.input.copy(selectedOption = it))
                    },
                    selectedOption = state.value.input.selectedOption,
                    isEnabled = !state.value.isDisabled,
                    isReadOnly = state.value.isReadOnly,
                    isRequired = state.value.isRequired,
                    hasError = state.value.hasError,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
