package app.k9mail.feature.funding.googleplay.ui.reminder

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import app.k9mail.feature.funding.api.FundingSettings
import kotlinx.datetime.Clock

class ActivityLifecycleObserver(
    private val settings: FundingSettings,
    private val clock: Clock = Clock.System,
) : FundingReminderContract.ActivityLifecycleObserver {

    private var lifecycleObserver: LifecycleObserver? = null

    override fun register(lifecycle: Lifecycle, onDestroy: () -> Unit) {
        lifecycleObserver = createLifecycleObserver(onDestroy)

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

    private fun createLifecycleObserver(onDestroy: () -> Unit): DefaultLifecycleObserver {
        return object : DefaultLifecycleObserver {
            private var startTime: Long = 0L

            override fun onResume(owner: LifecycleOwner) {
                super.onResume(owner)
                startTime = clock.now().toEpochMilliseconds()
            }

            override fun onPause(owner: LifecycleOwner) {
                super.onPause(owner)

                val endTime = clock.now().toEpochMilliseconds()
                val newActiveTime = endTime - startTime
                val oldActiveTime = settings.getActivityCounterInMillis()

                if (newActiveTime >= 0) {
                    settings.setActivityCounterInMillis(oldActiveTime + newActiveTime)
                }
            }

            override fun onDestroy(owner: LifecycleOwner) {
                super.onDestroy(owner)
                onDestroy()
            }
        }
    }
}
