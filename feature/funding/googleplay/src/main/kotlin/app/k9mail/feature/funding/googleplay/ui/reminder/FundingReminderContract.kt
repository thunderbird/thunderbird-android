package app.k9mail.feature.funding.googleplay.ui.reminder

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle

// 1 week in milliseconds
const val FUNDING_REMINDER_DELAY_MILLIS = 7 * 24 * 60 * 60 * 1000L
// 30 minutes in milliseconds
const val FUNDING_REMINDER_MIN_ACTIVITY_MILLIS = 30 * 60 * 1000L

interface FundingReminderContract {

    interface Reminder {
        fun registerReminder(activity: AppCompatActivity, onOpenFunding: () -> Unit)
    }

    fun interface Dialog {
        fun show(fragmentManager: FragmentManager)

        companion object {
            const val FRAGMENT_REQUEST_KEY = "funding_reminder_dialog"
            const val FRAGMENT_RESULT_SHOW_FUNDING = "show_funding"
        }
    }

    interface FragmentLifecycleObserver {
        fun register(fragmentManager: FragmentManager, onShow: () -> Unit)
        fun unregister(fragmentManager: FragmentManager)
    }

    interface ActivityLifecycleObserver {
        fun register(lifecycle: Lifecycle, onDestroy: () -> Unit)
        fun unregister(lifecycle: Lifecycle)
    }
}
