package net.thunderbird.app.common.feature.applock

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import net.thunderbird.feature.applock.api.AppLockGate

internal class AppLockActivityLifecycleCallbacks(
    private val gateFactory: AppLockGate.Factory?,
) : ActivityLifecycleCallbacks {

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (gateFactory == null) return
        if (activity !is FragmentActivity) return

        val gate = gateFactory.create(activity)
        activity.lifecycle.addObserver(gate)
    }

    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
