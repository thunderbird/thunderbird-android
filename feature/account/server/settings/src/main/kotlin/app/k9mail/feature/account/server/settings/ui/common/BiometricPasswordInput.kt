package app.k9mail.feature.account.server.settings.ui.common

import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import app.k9mail.core.ui.compose.designsystem.molecule.input.InputLayout
import app.k9mail.core.ui.compose.designsystem.molecule.input.PasswordInput
import app.k9mail.core.ui.compose.designsystem.molecule.input.inputContentPadding
import app.k9mail.feature.account.server.settings.R
import kotlinx.coroutines.delay
import app.k9mail.core.ui.compose.designsystem.R as RDesign

private const val SHOW_WARNING_DURATION = 5000L

/**
 * Variant of [PasswordInput] that only allows the password to be unmasked after the user has authenticated using
 * [BiometricPrompt].
 *
 * Note: Due to limitations of [BiometricPrompt] this composable can only be used inside a [FragmentActivity].
 */
@Composable
fun BiometricPasswordInput(
    onPasswordChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    password: String = "",
    isRequired: Boolean = false,
    errorMessage: String? = null,
    contentPadding: PaddingValues = inputContentPadding(),
) {
    var biometricWarning by remember { mutableStateOf<String?>(value = null) }

    LaunchedEffect(key1 = biometricWarning) {
        if (biometricWarning != null) {
            delay(SHOW_WARNING_DURATION)
            biometricWarning = null
        }
    }

    InputLayout(
        modifier = modifier,
        contentPadding = contentPadding,
        errorMessage = errorMessage,
        warningMessage = biometricWarning,
    ) {
        val title = stringResource(R.string.account_server_settings_password_authentication_title)
        val subtitle = stringResource(R.string.account_server_settings_password_authentication_subtitle)
        val needScreenLockMessage =
            stringResource(R.string.account_server_settings_password_authentication_screen_lock_required)

        TextFieldOutlinedPasswordBiometric(
            value = password,
            onValueChange = onPasswordChange,
            authenticationTitle = title,
            authenticationSubtitle = subtitle,
            needScreenLockMessage = needScreenLockMessage,
            onWarningChange = { biometricWarning = it?.toString() },
            label = stringResource(id = RDesign.string.designsystem_molecule_password_input_label),
            isRequired = isRequired,
            hasError = errorMessage != null,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
