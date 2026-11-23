package net.thunderbird.feature.funding.googleplay.ui.reminder

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragment
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import net.thunderbird.feature.funding.common.ui.reminder.FragmentLifecycleObserver
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FragmentLifecycleObserverRecreateTest {

    private val testSubject = FragmentLifecycleObserver(TARGET_FRAGMENT_TAG)

    @Test
    fun `should show when target fragment is removed`() {
        // Arrange
        val scenario = launchHostedFragment()
        var dialogShown = false
        scenario.onFragment { host ->
            val fragmentManager = host.parentFragmentManager
            testSubject.register(fragmentManager) {
                dialogShown = true
                DialogFragment().show(fragmentManager, null)
            }
        }

        // Act
        scenario.onFragment { host ->
            val fragmentManager = host.parentFragmentManager
            fragmentManager.beginTransaction()
                .remove(fragmentManager.requireTarget())
                .commitNow()
        }

        // Assert
        assertThat(dialogShown).isTrue()
    }

    @Test
    fun `should not show when fragment with different tag detaches`() {
        // Arrange
        val otherFragmentTag = "SOME_OTHER_FRAGMENT"
        val scenario = launchHostedFragment(tag = otherFragmentTag)
        var dialogShown = false
        scenario.onFragment { host ->
            val fragmentManager = host.parentFragmentManager
            testSubject.register(fragmentManager) {
                dialogShown = true
                DialogFragment().show(fragmentManager, null)
            }
        }

        // Act
        scenario.onFragment { host ->
            val fragmentManager = host.parentFragmentManager
            fragmentManager.beginTransaction()
                .remove(fragmentManager.requireTarget(tag = otherFragmentTag))
                .commitNow()
        }

        // Assert
        assertThat(dialogShown).isFalse()
    }

    /**
     * Issue 11867 resulted when the appeal dialog was triggered on configuration change,
     * which crashed as it occurred after the fragment manager had stopped.
     */
    @Test
    fun `should not show when activity is recreated`() {
        val scenario = launchHostedFragment()
        var dialogShown = false
        scenario.onFragment { host ->
            val fragmentManager = host.parentFragmentManager
            testSubject.register(fragmentManager) {
                dialogShown = true
                DialogFragment().show(fragmentManager, null)
            }
        }

        scenario.recreate()

        assertThat(dialogShown).isFalse()
    }

    private fun launchHostedFragment(tag: String = TARGET_FRAGMENT_TAG): FragmentScenario<Fragment> {
        val scenario = launchFragment<Fragment>()
        scenario.onFragment { host ->
            host.parentFragmentManager.beginTransaction()
                .add(Fragment(), tag)
                .commitNow()
        }
        return scenario
    }

    private fun FragmentManager.requireTarget(tag: String = TARGET_FRAGMENT_TAG): Fragment = requireNotNull(
        findFragmentByTag(tag),
    )

    private companion object {
        const val TARGET_FRAGMENT_TAG = "MessageViewContainerFragment"
    }
}
