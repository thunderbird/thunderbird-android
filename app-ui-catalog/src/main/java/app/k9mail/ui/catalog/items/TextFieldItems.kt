package app.k9mail.ui.catalog.items

import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import app.k9mail.core.ui.compose.designsystem.atom.textfield.TextFieldOutlined
import app.k9mail.core.ui.compose.designsystem.atom.textfield.TextFieldOutlinedEmailAddress
import app.k9mail.core.ui.compose.designsystem.atom.textfield.TextFieldOutlinedPassword

fun LazyGridScope.textFieldItems() {
    sectionHeaderItem(text = "Text fields")
    textFieldOutlinedItems()
    passwordTextFieldOutlinedItems()
    emailTextFieldOutlinedItems()
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
        WithRememberedInput(text = "") { input ->
            TextFieldOutlinedPassword(
                value = input.value,
                label = "Password",
                onValueChange = { input.value = it },
            )
        }
    }
    item {
        WithRememberedInput(text = "Password") { input ->
            TextFieldOutlinedPassword(
                value = input.value,
                label = "Password with error",
                onValueChange = { input.value = it },
                isError = true,
            )
        }
    }
    item {
        WithRememberedInput(text = "Password disabled") { input ->
            TextFieldOutlinedPassword(
                value = input.value,
                label = "Password",
                onValueChange = { input.value = it },
                enabled = false,
            )
        }
    }
}

private fun LazyGridScope.emailTextFieldOutlinedItems() {
    sectionSubtitleItem(text = "Email outlined")
    item {
        WithRememberedInput(text = "") { input ->
            TextFieldOutlinedEmailAddress(
                value = input.value,
                label = "Email address",
                onValueChange = { input.value = it },
            )
        }
    }
    item {
        WithRememberedInput(text = "email@example.com") { input ->
            TextFieldOutlinedEmailAddress(
                value = input.value,
                label = "Email address with error",
                onValueChange = { input.value = it },
                isError = true,
            )
        }
    }
    item {
        WithRememberedInput(text = "email@example.com disabled") { input ->
            TextFieldOutlinedEmailAddress(
                value = input.value,
                label = "Email address",
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
