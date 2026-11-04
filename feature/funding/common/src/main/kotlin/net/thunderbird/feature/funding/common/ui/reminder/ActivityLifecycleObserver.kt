package net.thunderbird.feature.funding.common.ui.reminder

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import net.thunderbird.feature.funding.common.api.FundingReminderContract

class ActivityLifecycleObserver : FundingReminderContract.ActivityLifecycleObserver {

    private var lifecycleObserver: LifecycleObserver? = null

    override fun register(lifecycle: Lifecycle, onDestroy: () -> Unit) {
        lifecycleObserver?.let {
            lifecycle.addObserver(it)
        }
    }

    override fun unregister(lifecycle: Lifecycle) {
        lifecycleObserver?.let {
            lifecycle.removeObserver(it)
            lifecycleObserver = null
        }
    }
}
