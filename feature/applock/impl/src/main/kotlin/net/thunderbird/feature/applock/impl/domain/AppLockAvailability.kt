package net.thunderbird.feature.applock.impl.domain

import net.thunderbird.feature.applock.api.UnavailableReason

/**
 * Checks whether authentication is available on this device.
 */
internal interface AppLockAvailability {
    fun isAuthenticationAvailable(): Boolean

    /**
     * Returns the reason why authentication is unavailable.
     * Only valid to call when [isAuthenticationAvailable] returns false.
     */
    fun getUnavailableReason(): UnavailableReason
}
