package app.k9mail.feature.account.server.settings.ui.common

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.textfield.TextFieldOutlinedPassword
import kotlinx.coroutines.launch
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.server.settings.ui.common.AuthenticationError
import net.thunderbird.feature.account.server.settings.ui.common.Authenticator

/**
 * Variant of [TextFieldOutlinedPassword] that only allows the
 * password to be unmasked after the user has authenticated using [Authenticator].
 */
@Suppress("LongParameterList")
@Composable
fun ProtectedTextFieldOutlinedPassword(
    value: String,
    onValueChange: (String) -> Unit,
    onWarningChange: (AuthenticationError?) -> Unit,
    authenticator: Authenticator,
    modifier: Modifier = Modifier,
    label: String? = null,
    isEnabled: Boolean = true,
    isReadOnly: Boolean = false,
    isRequired: Boolean = false,
    hasError: Boolean = false,
) {
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var isAuthenticated by rememberSaveable { mutableStateOf(false) }

    val activity = LocalActivity.current as Activity
    val scope = rememberCoroutineScope()

    TextFieldOutlinedPassword(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        isEnabled = isEnabled,
        isReadOnly = isReadOnly,
        isRequired = isRequired,
        hasError = hasError,
        isPasswordVisible = isPasswordVisible,
        onPasswordVisibilityToggleClicked = {
            if (isAuthenticated) {
                isPasswordVisible = !isPasswordVisible
                activity.setSecure(isPasswordVisible)
            } else {
                scope.launch {
                    when (val outcome = authenticator.authenticate()) {
                        is Outcome.Success -> {
                            isAuthenticated = true
                            isPasswordVisible = true
                            onWarningChange(null)
                            activity.setSecure(true)
                        }
                        is Outcome.Failure -> {
                            onWarningChange(outcome.error)
                        }
                    }
                }
            }
        },
    )

    DisposableEffect(key1 = "secureWindow") {
        activity.setSecure(isPasswordVisible)

        onDispose {
            activity.setSecure(false)
        }
    }
}

private fun Activity.setSecure(secure: Boolean) {
    window.setFlags(if (secure) WindowManager.LayoutParams.FLAG_SECURE else 0, WindowManager.LayoutParams.FLAG_SECURE)
}
