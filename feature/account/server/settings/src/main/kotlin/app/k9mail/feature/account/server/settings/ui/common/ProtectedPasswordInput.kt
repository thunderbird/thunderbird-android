package app.k9mail.feature.account.server.settings.ui.common

import androidx.annotation.StringRes
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
import app.k9mail.core.ui.compose.designsystem.molecule.input.InputLayout
import app.k9mail.core.ui.compose.designsystem.molecule.input.inputContentPadding
import app.k9mail.feature.account.server.settings.R
import kotlinx.coroutines.delay
import net.thunderbird.feature.account.server.settings.ui.common.AuthenticationError
import net.thunderbird.feature.account.server.settings.ui.common.Authenticator
import net.thunderbird.feature.account.server.settings.ui.common.rememberBiometricAuthenticator
import app.k9mail.core.ui.compose.designsystem.R as RDesign

private const val SHOW_WARNING_DURATION = 5000L

/**
 * Variant of [PasswordInput] that only allows the password to be unmasked after the user has authenticated using
 * [Authenticator] that defaults to [rememberBiometricAuthenticator].
 */
@Composable
fun ProtectedPasswordInput(
    onPasswordChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    password: String = "",
    isRequired: Boolean = false,
    errorMessage: String? = null,
    contentPadding: PaddingValues = inputContentPadding(),
    authenticator: Authenticator = rememberBiometricAuthenticator(
        title = stringResource(R.string.account_server_settings_password_authentication_title),
        subtitle = stringResource(R.string.account_server_settings_password_authentication_subtitle),
    ),
) {
    var authenticationError by remember { mutableStateOf<AuthenticationError?>(value = null) }
    val authenticationWarning = authenticationError?.let { stringResource(it.mapToStringRes()) }

    LaunchedEffect(key1 = authenticationError) {
        if (authenticationError != null) {
            delay(SHOW_WARNING_DURATION)
            authenticationError = null
        }
    }

    InputLayout(
        modifier = modifier,
        contentPadding = contentPadding,
        errorMessage = errorMessage,
        warningMessage = authenticationWarning,
    ) {
        ProtectedTextFieldOutlinedPassword(
            value = password,
            onValueChange = onPasswordChange,
            onWarningChange = { authenticationError = it },
            authenticator = authenticator,
            label = stringResource(id = RDesign.string.designsystem_molecule_password_input_label),
            isRequired = isRequired,
            hasError = errorMessage != null,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@StringRes
private fun AuthenticationError.mapToStringRes(): Int {
    return when (this) {
        AuthenticationError.NotAvailable ->
            R.string.account_server_settings_password_authentication_screen_lock_required
        AuthenticationError.Failed ->
            R.string.account_server_settings_password_authentication_failed
        AuthenticationError.UnableToStart ->
            R.string.account_server_settings_password_authentication_unable_to_start
    }
}
