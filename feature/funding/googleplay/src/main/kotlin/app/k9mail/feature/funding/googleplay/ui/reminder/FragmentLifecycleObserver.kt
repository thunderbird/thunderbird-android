package app.k9mail.feature.funding.googleplay.ui.reminder

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks

class FragmentLifecycleObserver(
    private val targetFragmentTag: String,
) : FundingReminderContract.FragmentLifecycleObserver {

    private var lifecycleCallbacks: FragmentLifecycleCallbacks? = null

    override fun register(fragmentManager: FragmentManager, onShow: () -> Unit) {
        lifecycleCallbacks = createFragmentLifecycleCallback(
            onCallback = {
                onShow()
                unregister(fragmentManager)
            },
        )

        // Register the lifecycle observer with the fragment manager.
        lifecycleCallbacks?.let {
            fragmentManager.registerFragmentLifecycleCallbacks(it, false)
        }
    }

    override fun unregister(fragmentManager: FragmentManager) {
        lifecycleCallbacks?.let {
            fragmentManager.unregisterFragmentLifecycleCallbacks(it)
            lifecycleCallbacks = null
        }
    }

    private fun createFragmentLifecycleCallback(onCallback: () -> Unit): FragmentLifecycleCallbacks {
        return object : FragmentLifecycleCallbacks() {
            override fun onFragmentDetached(fragmentManager: FragmentManager, fragment: Fragment) {
                super.onFragmentDetached(fragmentManager, fragment)

                if (fragment.tag == targetFragmentTag) {
                    onCallback()
                }
            }
        }
    }
}
