package net.thunderbird.ui.catalog.ui.page.molecule.items

import androidx.compose.foundation.lazy.grid.LazyGridScope
import app.k9mail.core.ui.compose.designsystem.molecule.input.CheckboxInput
import app.k9mail.core.ui.compose.designsystem.molecule.input.EmailAddressInput
import app.k9mail.core.ui.compose.designsystem.molecule.input.NumberInput
import app.k9mail.core.ui.compose.designsystem.molecule.input.PasswordInput
import app.k9mail.core.ui.compose.designsystem.molecule.input.SelectInput
import app.k9mail.core.ui.compose.designsystem.molecule.input.SwitchInput
import app.k9mail.core.ui.compose.designsystem.molecule.input.TextInput
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.ui.catalog.ui.page.common.helper.WithRememberedState
import net.thunderbird.ui.catalog.ui.page.common.list.ItemOutlinedView
import net.thunderbird.ui.catalog.ui.page.common.list.fullSpanItem
import net.thunderbird.ui.catalog.ui.page.common.list.sectionHeaderItem
import net.thunderbird.ui.catalog.ui.page.common.list.sectionSubtitleItem

@Suppress("LongMethod")
fun LazyGridScope.inputItems() {
    sectionHeaderItem(text = "TextInput")
    sectionSubtitleItem(text = "Default")
    fullSpanItem {
        ItemOutlinedView {
            WithRememberedState(input = "") { state ->
                TextInput(
                    text = state.value,
                    onTextChange = { state.value = it },
                )
            }
        }
    }
    sectionSubtitleItem(text = "With error")
    fullSpanItem {
        ItemOutlinedView {
            WithRememberedState(input = "") { state ->
                TextInput(
                    text = state.value,
                    onTextChange = { state.value = it },
                    errorMessage = "Invalid input",
                )
            }
        }
    }

    sectionHeaderItem(text = "EmailAddressInput")
    sectionSubtitleItem(text = "Default")
    fullSpanItem {
        ItemOutlinedView {
            WithRememberedState(input = "") { state ->
                EmailAddressInput(
                    emailAddress = state.value,
                    onEmailAddressChange = { state.value = it },
                )
            }
        }
    }
    sectionSubtitleItem(text = "With error")
    fullSpanItem {
        ItemOutlinedView {
            WithRememberedState(input = "wrong email address") { state ->
                EmailAddressInput(
                    emailAddress = state.value,
                    onEmailAddressChange = { state.value = it },
                    errorMessage = "Invalid email address",
                )
            }
        }
    }

    sectionHeaderItem(text = "NumberInput")
    sectionSubtitleItem(text = "Default")
    fullSpanItem {
        ItemOutlinedView {
            WithRememberedState<Long?>(input = null) { state ->
                NumberInput(
                    value = state.value,
                    onValueChange = { state.value = it },
                )
            }
        }
    }
    sectionSubtitleItem(text = "With error")
    fullSpanItem {
        ItemOutlinedView {
            WithRememberedState<Long?>(input = 123L) { state ->
                NumberInput(
                    value = state.value,
                    onValueChange = { state.value = it },
                    errorMessage = "Invalid number",
                )
            }
        }
    }

    sectionHeaderItem(text = "PasswordInput")
    sectionSubtitleItem(text = "Default")
    fullSpanItem {
        ItemOutlinedView {
            WithRememberedState(input = "") { state ->
                PasswordInput(
                    password = state.value,
                    onPasswordChange = { state.value = it },
                )
            }
        }
    }
    sectionSubtitleItem(text = "With error")
    fullSpanItem {
        ItemOutlinedView {
            WithRememberedState(input = "wrong password") { state ->
                PasswordInput(
                    password = state.value,
                    onPasswordChange = { state.value = it },
                    errorMessage = "Invalid password",
                )
            }
        }
    }

    sectionHeaderItem(text = "SelectInput")
    fullSpanItem {
        val options = persistentListOf("Option 1", "Option 2", "Option 3")
        ItemOutlinedView {
            WithRememberedState(input = options.first()) { state ->
                SelectInput(
                    options = options,
                    selectedOption = state.value,
                    onOptionChange = { state.value = it },
                )
            }
        }
    }

    sectionHeaderItem(text = "CheckboxInput")
    sectionSubtitleItem(text = "Default")
    fullSpanItem {
        ItemOutlinedView {
            WithRememberedState(input = false) { state ->
                CheckboxInput(
                    text = "Check the box",
                    checked = state.value,
                    onCheckedChange = { state.value = it },
                )
            }
        }
    }
    sectionSubtitleItem(text = "With error")
    fullSpanItem {
        ItemOutlinedView {
            WithRememberedState(input = false) { state ->
                CheckboxInput(
                    text = "Check the box",
                    checked = state.value,
                    onCheckedChange = { state.value = it },
                    errorMessage = "Checkbox must be checked",
                )
            }
        }
    }

    sectionHeaderItem(text = "SwitchInput")
    sectionSubtitleItem(text = "Default")
    fullSpanItem {
        ItemOutlinedView {
            WithRememberedState(input = false) { state ->
                SwitchInput(
                    text = "Switch the toggle",
                    checked = state.value,
                    onCheckedChange = { state.value = it },
                )
            }
        }
    }
    sectionSubtitleItem(text = "With error")
    fullSpanItem {
        ItemOutlinedView {
            WithRememberedState(input = false) { state ->
                SwitchInput(
                    text = "Switch the toggle",
                    checked = state.value,
                    onCheckedChange = { state.value = it },
                    errorMessage = "Switch must be checked",
                )
            }
        }
    }
}
