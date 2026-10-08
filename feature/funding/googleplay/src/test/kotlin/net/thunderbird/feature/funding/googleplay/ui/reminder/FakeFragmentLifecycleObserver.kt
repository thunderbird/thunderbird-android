package net.thunderbird.feature.funding.googleplay.ui.reminder

import androidx.fragment.app.FragmentManager
import net.thunderbird.feature.funding.common.api.FundingReminderContract

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
