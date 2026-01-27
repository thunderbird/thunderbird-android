package net.thunderbird.core.ui.setting.dialog.ui.components.dialog.value

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.molecule.input.AdvancedTextInput
import app.k9mail.core.ui.compose.theme2.MainTheme
import kotlinx.coroutines.delay
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.dialog.ui.components.dialog.SettingDialogLayout

// This a workaround for a bug in Compose, preventing the keyboard been show when requesting focus on a dialog,
// see: https://issuetracker.google.com/issues/204502668
private const val EDIT_TEXT_FOCUS_DELAY = 200L
private const val VALIDATION_DEBOUNCE_DELAY = 300L

@Composable
internal fun TextDialogView(
    setting: SettingValue.Text,
    onConfirmClick: (SettingValue<*>) -> Unit,
    onDismissClick: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(
            TextFieldValue(
                text = setting.value,
                selection = TextRange(setting.value.length),
            ),
        )
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        delay(EDIT_TEXT_FOCUS_DELAY)
        focusRequester.requestFocus()
    }

    LaunchedEffect(textFieldValue.text) {
        delay(VALIDATION_DEBOUNCE_DELAY)
        errorMessage = setting.validate(textFieldValue.text)
    }

    SettingDialogLayout(
        title = setting.title(),
        icon = setting.icon(),
        onConfirmClick = {
            val transformedText = setting.transform(textFieldValue.text)
            val validationError = setting.validate(transformedText)
            if (validationError == null) {
                onConfirmClick(setting.copy(value = transformedText))
            } else {
                errorMessage = validationError
            }
        },
        onDismissClick = onDismissClick,
        onDismissRequest = onDismissRequest,
        confirmButtonEnabled = errorMessage == null,
        modifier = modifier,
    ) {
        setting.description()?.let {
            TextBodyMedium(text = it)

            Spacer(modifier = Modifier.height(MainTheme.spacings.default))
        }

        AdvancedTextInput(
            text = textFieldValue,
            errorMessage = errorMessage,
            contentPadding = PaddingValues(),
            onTextChange = { changedText ->
                val transformedText = setting.transform(changedText.text)
                textFieldValue = changedText.copy(text = transformedText)
            },
            modifier = Modifier.focusRequester(focusRequester),
        )
    }
}
