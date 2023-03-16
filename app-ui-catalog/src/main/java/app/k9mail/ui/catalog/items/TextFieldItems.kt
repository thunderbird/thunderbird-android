package app.k9mail.ui.catalog.items

import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import app.k9mail.core.ui.compose.designsystem.atom.textfield.PasswordTextFieldOutlined
import app.k9mail.core.ui.compose.designsystem.atom.textfield.TextFieldOutlined

fun LazyGridScope.textFieldItems() {
    sectionHeaderItem(text = "Text fields")
    textFieldOutlinedItems()
    passwordTextFieldOutlinedItems()
}

private fun LazyGridScope.textFieldOutlinedItems() {
    sectionSubtitleItem(text = "Outlined")
    item {
        WithRememberedInput(text = "Initial text") { input ->
            TextFieldOutlined(
                value = input.value,
                label = "Label",
                onValueChange = { input.value = it },
            )
        }
    }
    item {
        WithRememberedInput(text = "Input text with error") { input ->
            TextFieldOutlined(
                value = input.value,
                label = "Label",
                onValueChange = { input.value = it },
                isError = true,
            )
        }
    }
    item {
        WithRememberedInput(text = "Input text disabled") { input ->
            TextFieldOutlined(
                value = input.value,
                label = "Label",
                onValueChange = { input.value = it },
                enabled = false,
            )
        }
    }
}

private fun LazyGridScope.passwordTextFieldOutlinedItems() {
    sectionSubtitleItem(text = "Password outlined")
    item {
        WithRememberedInput(text = "Input text") { input ->
            PasswordTextFieldOutlined(
                value = input.value,
                label = "Label",
                onValueChange = { input.value = it },
            )
        }
    }
    item {
        WithRememberedInput(text = "Input text with error") { input ->
            PasswordTextFieldOutlined(
                value = input.value,
                label = "Label",
                onValueChange = { input.value = it },
                isError = true,
            )
        }
    }
    item {
        WithRememberedInput(text = "Input text disabled") { input ->
            PasswordTextFieldOutlined(
                value = input.value,
                label = "Label",
                onValueChange = { input.value = it },
                enabled = false,
            )
        }
    }
}

@Composable
private fun WithRememberedInput(
    text: String,
    content: @Composable (text: MutableState<String>) -> Unit,
) {
    val inputText = remember { mutableStateOf(text) }
    content(inputText)
}
