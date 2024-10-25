package app.k9mail.feature.funding.googleplay.ui.reminder

import androidx.lifecycle.Lifecycle

class FakeActivityLifecycleObserver(
    var isRegistered: Boolean = false,
) : FundingReminderContract.ActivityLifecycleObserver {

    override fun register(lifecycle: Lifecycle, onDestroy: () -> Unit) {
        isRegistered = true
    }

    override fun unregister(lifecycle: Lifecycle) {
        isRegistered = false
    }
}
