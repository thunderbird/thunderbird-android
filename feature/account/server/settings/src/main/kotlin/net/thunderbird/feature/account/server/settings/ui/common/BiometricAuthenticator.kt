package net.thunderbird.feature.account.server.settings.ui.common

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import net.thunderbird.core.outcome.Outcome

/**
 * An [Authenticator] implementation that uses Android's BiometricPrompt to authenticate the user.
 *
 *  Note: Due to limitations of [androidx.biometric.BiometricPrompt] this composable can only be used inside a
 *  [androidx.fragment.app.FragmentActivity].
 *
 * @param activity The FragmentActivity context to use for the BiometricPrompt.
 * @param title The title to display on the biometric prompt.
 * @param subtitle The subtitle to display on the biometric prompt.
 * @param needScreenLockMessage The message to display when screen lock is required but not set
 */
class BiometricAuthenticator(
    private val activity: FragmentActivity,
    private val title: String,
    private val subtitle: String,
    private val needScreenLockMessage: String,
) : Authenticator {

    @Suppress("TooGenericExceptionCaught")
    override suspend fun authenticate(): Outcome<Unit, String> = suspendCancellableCoroutine { continuation ->
        val authenticationCallback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                if (continuation.isActive) continuation.resume(Outcome.Success(Unit))
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                val message: String = when (errorCode) {
                    BiometricPrompt.ERROR_HW_NOT_PRESENT,
                    BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL,
                    BiometricPrompt.ERROR_NO_BIOMETRICS,
                    -> needScreenLockMessage

                    else -> if (errString.isNotEmpty()) errString.toString() else "Authentication failed"
                }
                if (continuation.isActive) continuation.resume(Outcome.Failure(message))
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

        val executor = ContextCompat.getMainExecutor(activity)
        try {
            BiometricPrompt(activity, executor, authenticationCallback).authenticate(promptInfo)
        } catch (e: Exception) {
            val message: String = e.message ?: "Unable to start biometric prompt"
            if (continuation.isActive) continuation.resume(Outcome.Failure(message))
        }

        continuation.invokeOnCancellation {
            // No explicit cancellation support for BiometricPrompt
        }
    }
}
