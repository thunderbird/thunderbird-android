package app.k9mail.feature.funding.googleplay.ui.reminder

import androidx.fragment.app.FragmentManager

class FakeFragmentLifecycleObserver(
    var isRegistered: Boolean = false,
) : FundingReminderContract.FragmentLifecycleObserver {
    override fun register(fragmentManager: FragmentManager, onShow: () -> Unit) {
        isRegistered = true
        onShow()
    }

    override fun unregister(fragmentManager: FragmentManager) {
        isRegistered = false
    }
}
