package net.thunderbird.feature.applock.impl.domain

import androidx.lifecycle.DefaultLifecycleObserver

/**
 * Handles registration of lifecycle observers for app lock.
 * Abstracted for testability.
 */
internal interface AppLockLifecycleHandler {
    /**
     * Register the observer for app lifecycle and screen-off events.
     * The [onScreenOff] callback is invoked when screen turns off.
     */
    fun register(observer: DefaultLifecycleObserver, onScreenOff: () -> Unit)

    /**
     * Unregister the lifecycle observer and screen-off receiver.
     */
    fun unregister()
}
