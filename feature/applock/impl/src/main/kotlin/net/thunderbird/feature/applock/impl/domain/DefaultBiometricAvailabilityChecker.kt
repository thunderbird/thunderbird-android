package net.thunderbird.feature.applock.impl.domain

import androidx.biometric.BiometricManager
import net.thunderbird.feature.applock.api.UnavailableReason

/**
 * Default implementation using Android's BiometricManager.
 */
internal class DefaultBiometricAvailabilityChecker(
    private val biometricManager: BiometricManager,
) : AppLockAvailability {

    override fun isAuthenticationAvailable(): Boolean {
        return BiometricAuthenticator.isAvailable(biometricManager)
    }

    override fun getUnavailableReason(): UnavailableReason {
        return when (biometricManager.canAuthenticate(allowedAuthenticators())) {
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> UnavailableReason.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> UnavailableReason.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> UnavailableReason.TEMPORARILY_UNAVAILABLE
            else -> UnavailableReason.UNKNOWN
        }
    }
}
