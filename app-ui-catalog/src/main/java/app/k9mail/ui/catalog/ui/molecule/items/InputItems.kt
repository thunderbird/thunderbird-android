package app.k9mail.ui.catalog.ui.molecule.items

import androidx.compose.foundation.lazy.grid.LazyGridScope
import app.k9mail.core.ui.compose.designsystem.molecule.input.CheckboxInput
import app.k9mail.core.ui.compose.designsystem.molecule.input.EmailAddressInput
import app.k9mail.core.ui.compose.designsystem.molecule.input.PasswordInput
import app.k9mail.core.ui.compose.designsystem.molecule.input.SelectInput
import app.k9mail.core.ui.compose.designsystem.molecule.input.SwitchInput
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.ui.catalog.ui.common.helper.WithRememberedState
import app.k9mail.ui.catalog.ui.common.list.ItemOutlined
import app.k9mail.ui.catalog.ui.common.list.sectionHeaderItem
import app.k9mail.ui.catalog.ui.common.list.sectionSubtitleItem
import kotlinx.collections.immutable.persistentListOf

@Suppress("LongMethod")
fun LazyGridScope.inputItems() {
    sectionHeaderItem(text = "EmailAddressInput")
    sectionSubtitleItem(text = "Default")
    item {
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
    item {
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

    sectionHeaderItem(text = "PasswordInput")
    sectionSubtitleItem(text = "Default")
    item {
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
    item {
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
    item {
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
    item {
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

    sectionHeaderItem(text = "SelectInput")
    item {
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
}
