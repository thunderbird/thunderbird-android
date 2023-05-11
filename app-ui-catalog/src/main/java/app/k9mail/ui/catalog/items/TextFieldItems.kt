package app.k9mail.ui.catalog.items

import androidx.compose.foundation.lazy.grid.LazyGridScope
import app.k9mail.core.ui.compose.designsystem.atom.textfield.TextFieldOutlined
import app.k9mail.core.ui.compose.designsystem.atom.textfield.TextFieldOutlinedEmailAddress
import app.k9mail.core.ui.compose.designsystem.atom.textfield.TextFieldOutlinedPassword
import app.k9mail.ui.catalog.helper.WithRememberedState

fun LazyGridScope.textFieldItems() {
    sectionHeaderItem(text = "Text fields")
    textFieldOutlinedItems()
    passwordTextFieldOutlinedItems()
    emailTextFieldOutlinedItems()
}

private fun LazyGridScope.textFieldOutlinedItems() {
    sectionSubtitleItem(text = "Outlined")
    item {
        WithRememberedState(input = "Initial text") { state ->
            TextFieldOutlined(
                value = state.value,
                label = "Label",
                onValueChange = { state.value = it },
            )
        }
    }
    item {
        WithRememberedState(input = "Input text with error") { state ->
            TextFieldOutlined(
                value = state.value,
                label = "Label",
                onValueChange = { state.value = it },
                isError = true,
            )
        }
    }
    item {
        WithRememberedState(input = "Input text disabled") { state ->
            TextFieldOutlined(
                value = state.value,
                label = "Label",
                onValueChange = { state.value = it },
                enabled = false,
            )
        }
    }
}

private fun LazyGridScope.passwordTextFieldOutlinedItems() {
    sectionSubtitleItem(text = "Password outlined")
    item {
        WithRememberedState(input = "") { state ->
            TextFieldOutlinedPassword(
                value = state.value,
                label = "Password",
                onValueChange = { state.value = it },
            )
        }
    }
    item {
        WithRememberedState(input = "Password") { state ->
            TextFieldOutlinedPassword(
                value = state.value,
                label = "Password with error",
                onValueChange = { state.value = it },
                isError = true,
            )
        }
    }
    item {
        WithRememberedState(input = "Password disabled") { state ->
            TextFieldOutlinedPassword(
                value = state.value,
                label = "Password",
                onValueChange = { state.value = it },
                enabled = false,
            )
        }
    }
}

private fun LazyGridScope.emailTextFieldOutlinedItems() {
    sectionSubtitleItem(text = "Email outlined")
    item {
        WithRememberedState(input = "") { state ->
            TextFieldOutlinedEmailAddress(
                value = state.value,
                label = "Email address",
                onValueChange = { state.value = it },
            )
        }
    }
    item {
        WithRememberedState(input = "email@example.com") { state ->
            TextFieldOutlinedEmailAddress(
                value = state.value,
                label = "Email address with error",
                onValueChange = { state.value = it },
                isError = true,
            )
        }
    }
    item {
        WithRememberedState(input = "email@example.com disabled") { state ->
            TextFieldOutlinedEmailAddress(
                value = state.value,
                label = "Email address",
                onValueChange = { state.value = it },
                enabled = false,
            )
        }
    }
}
