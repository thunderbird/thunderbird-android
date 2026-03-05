package net.thunderbird.feature.applock.api

/**
 * Authentication error types that can occur during the authentication process.
 */
sealed interface AppLockError {
    /**
     * Device authentication is not available on this device.
     */
    data object NotAvailable : AppLockError

    /**
     * User has not enrolled any biometric credentials or device credentials.
     */
    data object NotEnrolled : AppLockError

    /**
     * Authentication attempt failed.
     */
    data object Failed : AppLockError

    /**
     * User explicitly canceled the authentication dialog.
     */
    data object Canceled : AppLockError

    /**
     * Authentication was interrupted by the system (e.g., app went to background,
     * configuration change, or fragment lifecycle). Should retry silently.
     */
    data object Interrupted : AppLockError

    /**
     * Too many failed attempts, user is locked out.
     *
     * @property durationSeconds The duration of the lockout:
     *   - `> 0`: Temporary lockout with known duration in seconds
     *   - `== 0`: Temporary lockout with unknown duration
     *   - `< 0`: Permanent lockout (user must unlock device with PIN/pattern/password to reset)
     */
    data class Lockout(val durationSeconds: Int) : AppLockError {
        companion object {
            const val DURATION_UNKNOWN = 0
            const val DURATION_PERMANENT = -1
        }
    }

    /**
     * Unable to start the authentication system.
     *
     * @property message A description of why authentication could not start.
     */
    data class UnableToStart(val message: String) : AppLockError
}
