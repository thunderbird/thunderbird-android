package net.thunderbird.feature.applock.api

/**
 * Functional interface for authenticating a user.
 *
 * This abstraction allows for different authentication implementations
 * (biometric, device credential, etc.) and testing with fakes.
 */
fun interface AppLockAuthenticator {
    /**
     * Authenticate the user.
     *
     * @return An [AppLockResult] representing the outcome.
     */
    suspend fun authenticate(): AppLockResult
}
