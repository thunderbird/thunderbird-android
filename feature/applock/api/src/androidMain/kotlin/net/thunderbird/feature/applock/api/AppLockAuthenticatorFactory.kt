package net.thunderbird.feature.applock.api

import androidx.fragment.app.FragmentActivity

/**
 * Factory for creating [AppLockAuthenticator] instances bound to a specific activity.
 *
 * This allows modules that depend on the API to create authenticators without
 * depending on the concrete implementation (e.g., BiometricAuthenticator).
 */
fun interface AppLockAuthenticatorFactory {
    fun create(activity: FragmentActivity): AppLockAuthenticator
}
