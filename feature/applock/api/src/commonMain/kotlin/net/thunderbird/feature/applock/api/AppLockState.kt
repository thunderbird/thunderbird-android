package net.thunderbird.feature.applock.api

/**
 * Unified state for the app lock feature.
 */
sealed interface AppLockState {
    /**
     * App lock is disabled by user preference - no authentication required.
     */
    data object Disabled : AppLockState

    /**
     * App lock is enabled but authentication is unavailable on this device.
     * Access is blocked (fail-closed). User should be shown guidance to restore
     * authentication availability, plus an explicit option to close the app.
     *
     * @property reason Why authentication is unavailable.
     */
    data class Unavailable(val reason: UnavailableReason) : AppLockState

    /**
     * App lock is enabled and authentication is required.
     */
    data object Locked : AppLockState

    /**
     * Authentication is currently in progress.
     *
     * @property attemptId Internal identifier for correlating auth results.
     */
    data class Unlocking(
        val attemptId: Long,
    ) : AppLockState

    /**
     * User has successfully authenticated.
     *
     * @property lastHiddenAtElapsedMillis Elapsed realtime when app went to background, or null if visible.
     */
    data class Unlocked(
        val lastHiddenAtElapsedMillis: Long? = null,
    ) : AppLockState

    /**
     * Authentication failed with an error.
     *
     * @property error The error from the failed authentication attempt.
     */
    data class Failed(val error: AppLockError) : AppLockState
}

/**
 * Reason why authentication is unavailable on this device.
 */
enum class UnavailableReason {
    /**
     * Device does not have biometric or credential hardware.
     */
    NO_HARDWARE,

    /**
     * User has not enrolled any biometrics or device credentials.
     */
    NOT_ENROLLED,

    /**
     * Authentication hardware is temporarily unavailable.
     * Usually resolves without user setup changes.
     */
    TEMPORARILY_UNAVAILABLE,

    /**
     * Authentication is unavailable for an unknown reason.
     */
    UNKNOWN,
}

/**
 * Check if the app is unlocked (authenticated or lock disabled by user).
 *
 * Note: [AppLockState.Unavailable] is NOT considered unlocked - it blocks access
 * because lock was enabled but authentication became unavailable.
 */
fun AppLockState.isUnlocked(): Boolean {
    return this is AppLockState.Unlocked || this is AppLockState.Disabled
}
