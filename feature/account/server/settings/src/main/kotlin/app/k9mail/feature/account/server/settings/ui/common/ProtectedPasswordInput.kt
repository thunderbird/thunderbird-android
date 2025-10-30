package app.k9mail.feature.account.server.settings.ui.common

import androidx.activity.compose.LocalActivity
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
import net.thunderbird.feature.account.server.settings.ui.common.Authenticator
import net.thunderbird.feature.account.server.settings.ui.common.BiometricAuthenticator
import app.k9mail.core.ui.compose.designsystem.R as RDesign

private const val SHOW_WARNING_DURATION = 5000L

/**
 * Variant of [PasswordInput] that only allows the password to be unmasked after the user has authenticated using
 * [Authenticator] that defaults to [BiometricAuthenticator].
 */
@Composable
fun ProtectedPasswordInput(
    onPasswordChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    password: String = "",
    isRequired: Boolean = false,
    errorMessage: String? = null,
    contentPadding: PaddingValues = inputContentPadding(),
    authenticator: Authenticator? = null,
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

        val resolvedAuthenticator: Authenticator = authenticator ?: run {
            val activity = LocalActivity.current as androidx.fragment.app.FragmentActivity
            BiometricAuthenticator(
                activity = activity,
                title = title,
                subtitle = subtitle,
                needScreenLockMessage = needScreenLockMessage,
            )
        }

        ProtectedTextFieldOutlinedPassword(
            value = password,
            onValueChange = onPasswordChange,
            onWarningChange = { biometricWarning = it?.toString() },
            authenticator = resolvedAuthenticator,
            label = stringResource(id = RDesign.string.designsystem_molecule_password_input_label),
            isRequired = isRequired,
            hasError = errorMessage != null,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
