package net.thunderbird.app.common.activity

import android.app.Dialog
import android.os.Bundle
import android.os.Looper
import android.view.KeyEvent
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isSameInstanceAs
import net.thunderbird.core.android.testing.RobolectricTest
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@Config(sdk = [26, 31, 36])
class KeypadDialogFragmentLifecycleCallbacksTest : RobolectricTest() {
    private val testSubject = KeypadNavigationActivityLifecycleCallbacks(
        RuntimeEnvironment.getApplication(),
        FakeKeypadInteractionPreferences(),
    ).apply { register() }

    @Test
    fun `activity registration should also install navigation on independent fragment dialog windows`() {
        // Arrange
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val fragment = KeypadTestDialogFragment()
        testSubject.onActivityCreated(activity, null)

        // Act
        fragment.showNow(activity.supportFragmentManager, "keypad-dialog")
        val dialog = fragment.requireDialog()
        val callback = requireNotNull(dialog.window?.callback)

        // Assert
        assertThat(callback).isInstanceOf<KeypadDialogWindowCallback>()
        assertThat((callback as KeypadDialogWindowCallback).delegate).isSameInstanceAs(dialog)

        // Act
        focusKeypadDialogWindow(dialog)
        callback.dispatchKeyEvent(keypadEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F2))
        callback.dispatchKeyEvent(keypadEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_F2))
        shadowOf(Looper.getMainLooper()).idle()

        // Assert
        assertThat(dialog.isShowing).isFalse()
    }

    @Test
    fun `recursive registration should include dialogs launched from child fragments`() {
        // Arrange
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val parent = Fragment()
        activity.supportFragmentManager.beginTransaction().add(parent, "parent").commitNow()
        val fragment = KeypadTestDialogFragment()

        // Act
        fragment.showNow(parent.childFragmentManager, "child-dialog")

        // Assert
        assertThat(fragment.requireDialog().window?.callback).isNotNull().isInstanceOf<KeypadDialogWindowCallback>()
    }

    @Test
    fun `stopping and restarting a dialog should restore and reinstall only one wrapper`() {
        // Arrange
        val activityController = Robolectric.buildActivity(FragmentActivity::class.java).setup()
        val activity = activityController.get()
        val fragment = KeypadTestDialogFragment()
        fragment.showNow(activity.supportFragmentManager, "keypad-dialog")
        val dialog = fragment.requireDialog()
        val originalWrapper = dialog.window?.callback as KeypadDialogWindowCallback
        val originalDelegate = originalWrapper.delegate

        // Act
        activityController.pause().stop()

        // Assert
        assertThat(dialog.window?.callback).isSameInstanceAs(originalDelegate)

        // Act
        activityController.restart().start().resume()
        val newWrapper = dialog.window?.callback

        // Assert
        assertThat(newWrapper).isNotNull().isInstanceOf<KeypadDialogWindowCallback>()
        assertThat((newWrapper as KeypadDialogWindowCallback).delegate).isSameInstanceAs(originalDelegate)
    }
}

class KeypadTestDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = Dialog(requireContext())
}
