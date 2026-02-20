package net.thunderbird.feature.applock.api

import kotlinx.coroutines.flow.StateFlow

/**
 * Coordinates app lock flow and orchestration.
 *
 * This is the main public API for the app lock feature. Other modules should
 * only interact with app lock through this interface.
 *
 * Uses a pull model: Activities observe [state] and call [ensureUnlocked] when
 * they need to ensure the app is unlocked. No effect bus is used for prompting.
 *
 * **Threading contract:** All methods must be called on the main thread.
 * State mutations are not thread-safe. Callers must not invoke methods from
 * background threads or coroutine dispatchers other than [Dispatchers.Main].
 */
interface AppLockCoordinator {
    /**
     * Observable app lock state for UI rendering.
     */
    val state: StateFlow<AppLockState>

    /**
     * Current app lock configuration.
     */
    val config: AppLockConfig

    /**
     * Whether app lock is currently enabled in settings.
     */
    val isEnabled: Boolean
        get() = config.isEnabled

    /**
     * Whether authentication (biometric or device credential) is available on this device.
     */
    val isAuthenticationAvailable: Boolean

    /**
     * Notify that the app came to foreground.
     */
    fun onAppForegrounded()

    /**
     * Notify that the app went to background.
     */
    fun onAppBackgrounded()

    /**
     * Notify that the screen turned off. Immediately locks the app if enabled.
     */
    fun onScreenOff()

    /**
     * Lock the app immediately.
     */
    fun lockNow()

    /**
     * Request unlock.
     *
     * Call this from Activity.onResume() when state is not Unlocked/Disabled.
     * Transitions Locked/Failed → Unlocking if not already unlocking.
     *
     * @return true if unlock was initiated or already unlocked/disabled,
     *         false if already unlocking (caller should wait, not show duplicate prompt)
     */
    fun ensureUnlocked(): Boolean

    /**
     * Update app lock configuration.
     */
    fun onSettingsChanged(config: AppLockConfig)

    /**
     * Authenticate using the provided authenticator.
     * Call this when state is [AppLockState.Unlocking].
     */
    suspend fun authenticate(authenticator: AppLockAuthenticator): AppLockResult

    /**
     * Authenticate and enable app lock in a single operation.
     *
     * Unlike [onSettingsChanged], this authenticates *before* persisting the config change.
     * On success, config is persisted with `isEnabled = true` and state transitions to Unlocked.
     * On failure, no config or state change occurs.
     *
     * @return [AppLockResult] indicating success or the authentication error.
     */
    suspend fun requestEnable(authenticator: AppLockAuthenticator): AppLockResult

    /**
     * Re-check authentication availability after returning from device settings.
     * Transitions Unavailable -> Locked if auth is now available.
     */
    fun refreshAvailability()
}
