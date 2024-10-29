package app.k9mail.feature.funding.googleplay.ui.reminder

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle

// TODO These values are temporarily reduced for testing in 8.0b5. This should be 7 days and 30
// mintues under normal circumstances

// 1 day in milliseconds
const val FUNDING_REMINDER_DELAY_MILLIS = 1 * 24 * 60 * 60 * 1000L
// 15 minutes in milliseconds
const val FUNDING_REMINDER_MIN_ACTIVITY_MILLIS = 15 * 60 * 1000L

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
