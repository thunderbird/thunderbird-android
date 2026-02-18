package net.thunderbird.app.common.activity

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import net.thunderbird.core.android.common.activity.ActivityProvider
import net.thunderbird.core.logging.Logger

private const val TAG = "DefaultActivityProvider"

/**
 * ActivityProvider implementation that tracks the current resumed activity and whether the app is in the foreground.
 */
class DefaultActivityProvider(
    application: Application,
    private val logger: Logger,
) : Application.ActivityLifecycleCallbacks, ActivityProvider, DefaultLifecycleObserver {
    @Volatile
    private var lastResumed: Activity? = null

    @Volatile
    private var inForeground: Boolean = false

    override fun getCurrent(): Activity? = if (inForeground) lastResumed else null

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        application.registerActivityLifecycleCallbacks(this)
    }

    // ProcessLifecycleOwner callbacks
    override fun onResume(owner: LifecycleOwner) {
        logger.debug(TAG) { "App in foreground" }
        inForeground = true
    }

    override fun onPause(owner: LifecycleOwner) {
        logger.debug(TAG) { "App in background" }
        inForeground = false
        lastResumed = null
    }

    // ActivityLifecycleCallbacks
    override fun onActivityResumed(activity: Activity) {
        logger.debug(TAG) { "Resumed: setting activity to ${activity::class.java.simpleName}" }
        lastResumed = activity
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (lastResumed === activity) {
            logger.debug(TAG) { "Activity destroyed, clearing last resumed" }
            lastResumed = null
        }
    }

    override fun onActivityCreated(a: Activity, b: Bundle?) = Unit
    override fun onActivityStarted(a: Activity) = Unit
    override fun onActivityPaused(a: Activity) = Unit
    override fun onActivityStopped(a: Activity) = Unit
    override fun onActivitySaveInstanceState(a: Activity, outState: Bundle) = Unit
}
