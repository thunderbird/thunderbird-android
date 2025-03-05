package app.k9mail.feature.account.server.settings.ui.common

import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import app.k9mail.core.ui.compose.designsystem.atom.textfield.TextFieldOutlinedPassword

/**
 * Variant of [TextFieldOutlinedPassword] that only allows the password to be unmasked after the user has authenticated
 * using [BiometricPrompt].
 *
 * Note: Due to limitations of [BiometricPrompt] this composable can only be used inside a [FragmentActivity].
 */
@Suppress("LongParameterList")
@Composable
fun TextFieldOutlinedPasswordBiometric(
    value: String,
    onValueChange: (String) -> Unit,
    authenticationTitle: String,
    authenticationSubtitle: String,
    needScreenLockMessage: String,
    onWarningChange: (CharSequence?) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    isEnabled: Boolean = true,
    isReadOnly: Boolean = false,
    isRequired: Boolean = false,
    hasError: Boolean = false,
) {
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var isAuthenticated by rememberSaveable { mutableStateOf(false) }
    var isAuthenticationRequired by rememberSaveable { mutableStateOf(true) }

    // If the entire password was removed, we allow the user to unmask the text field without requiring authentication.
    if (value.isEmpty()) {
        isAuthenticationRequired = false
    }

    val activity = LocalActivity.current as FragmentActivity

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
            if (!isAuthenticationRequired || isAuthenticated) {
                isPasswordVisible = !isPasswordVisible
                activity.setSecure(isPasswordVisible)
            } else {
                showBiometricPrompt(
                    activity,
                    authenticationTitle,
                    authenticationSubtitle,
                    needScreenLockMessage,
                    onAuthSuccess = {
                        isAuthenticated = true
                        isPasswordVisible = true
                        onWarningChange(null)
                        activity.setSecure(true)
                    },
                    onAuthError = onWarningChange,
                )
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

private fun showBiometricPrompt(
    activity: FragmentActivity,
    title: String,
    subtitle: String,
    needScreenLockMessage: String,
    onAuthSuccess: () -> Unit,
    onAuthError: (CharSequence) -> Unit,
) {
    val authenticationCallback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            onAuthSuccess()
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            if (errorCode == BiometricPrompt.ERROR_HW_NOT_PRESENT ||
                errorCode == BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL ||
                errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS
            ) {
                onAuthError(needScreenLockMessage)
            } else if (errString.isNotEmpty()) {
                onAuthError(errString)
            }
        }
    }

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL,
        )
        .setTitle(title)
        .setSubtitle(subtitle)
        .build()

    BiometricPrompt(activity, authenticationCallback).authenticate(promptInfo)
}

private fun FragmentActivity.setSecure(secure: Boolean) {
    window.setFlags(if (secure) WindowManager.LayoutParams.FLAG_SECURE else 0, WindowManager.LayoutParams.FLAG_SECURE)
}
