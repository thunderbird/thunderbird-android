package net.thunderbird.feature.applock.api

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver

/**
 * Lifecycle-aware component that handles app lock UI and authentication.
 *
 * Add this observer to an Activity's lifecycle to automatically:
 * - Show/hide a lock overlay when the app is locked
 * - Trigger biometric authentication when needed
 * - Handle authentication results
 *
 * Usage:
 * ```
 * class MyActivity : AppCompatActivity() {
 *     private val appLockGate: AppLockGate by inject { parametersOf(this) }
 *
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         lifecycle.addObserver(appLockGate)
 *     }
 * }
 * ```
 */
interface AppLockGate : DefaultLifecycleObserver {
    /**
     * Factory for creating [AppLockGate] instances bound to a specific activity.
     */
    interface Factory {
        /**
         * Create an [AppLockGate] for the given activity.
         *
         * @param activity The FragmentActivity to bind to (needed for BiometricPrompt)
         */
        fun create(activity: FragmentActivity): AppLockGate
    }
}
