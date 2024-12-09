package app.k9mail.feature.funding.googleplay.ui.reminder

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.Test
import org.mockito.ArgumentMatchers.eq
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions

class FragmentLifecycleObserverTest {

    @Test
    fun `should call onShow and unregister when target fragment is detached`() {
        val targetFragmentTag = "targetFragment"
        val fragmentManager = mock<FragmentManager>()
        val fragment = mock<Fragment> {
            on { tag } doReturn targetFragmentTag
        }
        var showed = false
        val onShow = { showed = true }
        val observer = FragmentLifecycleObserver(targetFragmentTag)

        observer.register(fragmentManager, onShow)
        val lifecycleCallbacksCaptor = argumentCaptor<FragmentManager.FragmentLifecycleCallbacks>()
        verify(fragmentManager).registerFragmentLifecycleCallbacks(lifecycleCallbacksCaptor.capture(), eq(false))

        // Simulate fragment detached
        lifecycleCallbacksCaptor.firstValue.onFragmentDetached(fragmentManager, fragment)

        assertThat(showed).isTrue()
        verify(fragmentManager).unregisterFragmentLifecycleCallbacks(lifecycleCallbacksCaptor.firstValue)
    }

    @Test
    fun `should not call onShow when target fragment is not detached`() {
        val targetFragmentTag = "targetFragment"
        val fragmentManager = mock<FragmentManager>()
        val fragment = mock<Fragment> {
            on { tag } doReturn "otherFragment"
        }
        var showed = false
        val onShow = { showed = true }
        val observer = FragmentLifecycleObserver(targetFragmentTag)

        observer.register(fragmentManager, onShow)
        val lifecycleCallbacksCaptor = argumentCaptor<FragmentManager.FragmentLifecycleCallbacks>()
        verify(fragmentManager).registerFragmentLifecycleCallbacks(lifecycleCallbacksCaptor.capture(), eq(false))

        // Simulate fragment detached
        lifecycleCallbacksCaptor.firstValue.onFragmentDetached(fragmentManager, fragment)

        assertThat(showed).isFalse()
        verifyNoMoreInteractions(fragmentManager)
    }

    @Test
    fun `should remove callback when unregister is called`() {
        val targetFragmentTag = "targetFragment"
        val fragmentManager = mock<FragmentManager>()
        val fragment = mock<Fragment> {
            on { tag } doReturn targetFragmentTag
        }
        val onShow = { }
        val observer = FragmentLifecycleObserver(targetFragmentTag)

        observer.register(fragmentManager, onShow)
        val lifecycleCallbacksCaptor = argumentCaptor<FragmentManager.FragmentLifecycleCallbacks>()
        verify(fragmentManager).registerFragmentLifecycleCallbacks(lifecycleCallbacksCaptor.capture(), eq(false))

        observer.unregister(fragmentManager)
        lifecycleCallbacksCaptor.firstValue.onFragmentDetached(fragmentManager, fragment)

        verify(fragmentManager).unregisterFragmentLifecycleCallbacks(lifecycleCallbacksCaptor.firstValue)
    }
}
