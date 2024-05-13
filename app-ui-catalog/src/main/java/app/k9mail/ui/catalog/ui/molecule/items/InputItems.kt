package app.k9mail.ui.catalog.ui.molecule.items

import androidx.compose.foundation.lazy.grid.LazyGridScope
import app.k9mail.core.ui.compose.designsystem.molecule.input.CheckboxInput
import app.k9mail.core.ui.compose.designsystem.molecule.input.EmailAddressInput
import app.k9mail.core.ui.compose.designsystem.molecule.input.NumberInput
import app.k9mail.core.ui.compose.designsystem.molecule.input.PasswordInput
import app.k9mail.core.ui.compose.designsystem.molecule.input.SelectInput
import app.k9mail.core.ui.compose.designsystem.molecule.input.SwitchInput
import app.k9mail.core.ui.compose.designsystem.molecule.input.TextInput
import app.k9mail.ui.catalog.ui.common.helper.WithRememberedState
import app.k9mail.ui.catalog.ui.common.list.ItemOutlined
import app.k9mail.ui.catalog.ui.common.list.fullSpanItem
import app.k9mail.ui.catalog.ui.common.list.sectionHeaderItem
import app.k9mail.ui.catalog.ui.common.list.sectionSubtitleItem
import kotlinx.collections.immutable.persistentListOf

@Suppress("LongMethod")
fun LazyGridScope.inputItems() {
    sectionHeaderItem(text = "TextInput")
    sectionSubtitleItem(text = "Default")
    fullSpanItem {
        ItemOutlined {
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
        ItemOutlined {
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
        ItemOutlined {
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
        ItemOutlined {
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
        ItemOutlined {
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
        ItemOutlined {
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
        ItemOutlined {
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
        ItemOutlined {
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
        ItemOutlined {
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
        ItemOutlined {
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
        ItemOutlined {
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
        ItemOutlined {
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
        ItemOutlined {
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
