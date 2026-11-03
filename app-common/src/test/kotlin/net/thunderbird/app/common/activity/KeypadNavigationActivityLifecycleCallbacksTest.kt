package net.thunderbird.app.common.activity

import android.view.KeyEvent
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isSameInstanceAs
import assertk.assertions.isTrue
import net.thunderbird.core.android.testing.RobolectricTest
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

@Config(sdk = [26, 31, 36])
class KeypadNavigationActivityLifecycleCallbacksTest : RobolectricTest() {
    private val interactionPreferences = FakeKeypadInteractionPreferences()
    private val testSubject = KeypadNavigationActivityLifecycleCallbacks(
        RuntimeEnvironment.getApplication(),
        interactionPreferences,
    ).apply { register() }

    @Test
    fun `registered lifecycle callback should install once and retain the original window callback`() {
        // Arrange
        val activityController = Robolectric.buildActivity(RecordingKeypadActivity::class.java).create().start()
        val activity = activityController.get()
        val originalCallback = activity.window.callback

        // Act
        activityController.resume()
        val firstCallback = activity.window.callback
        testSubject.onActivityResumed(activity)
        activityController.pause().resume()

        // Assert
        assertThat(firstCallback).isInstanceOf<KeypadWindowCallback>()
        assertThat(activity.window.callback).isSameInstanceAs(firstCallback)
        assertThat((firstCallback as KeypadWindowCallback).delegate).isSameInstanceAs(originalCallback)
    }

    @Test
    fun `pausing the activity should discard the unfinished shortcut before resuming`() {
        // Arrange
        val activityController = Robolectric.buildActivity(RecordingKeypadActivity::class.java).create().start()
        val activity = activityController.get()
        val delegate = RecordingKeypadWindowCallback(activity)
        activity.window.callback = delegate
        activityController.resume()
        val callback = activity.window.callback
        callback.dispatchKeyEvent(keypadEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F1))

        // Act
        activityController.pause().resume()
        val staleRelease = keypadEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_F1)
        activity.window.callback.dispatchKeyEvent(staleRelease)

        // Assert
        assertThat(activity.menuOpenCount).isEqualTo(0)
        assertThat(delegate.events).containsExactly(staleRelease)
        assertThat(ShadowDialog.getShownDialogs()).isEmpty()

        // Act
        activity.window.callback.dispatchKeyEvent(keypadEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F1))
        activity.window.callback.dispatchKeyEvent(keypadEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_F1))

        // Assert
        assertThat(ShadowDialog.getLatestDialog().isShowing).isTrue()
        assertThat(ShadowDialog.getShownDialogs().size).isEqualTo(1)
        assertThat(delegate.events).containsExactly(staleRelease)
    }

    @Test
    fun `destroying the activity should restore its original callback`() {
        // Arrange
        val activityController = Robolectric.buildActivity(RecordingKeypadActivity::class.java).create().start()
        val activity = activityController.get()
        val originalCallback = activity.window.callback
        activityController.resume()

        // Act
        activityController.pause().stop().destroy()

        // Assert
        assertThat(activity.window.callback).isSameInstanceAs(originalCallback)
    }

    @Test
    fun `destroy should leave an independently replaced window callback intact`() {
        // Arrange
        val activityController = Robolectric.buildActivity(
            RecordingKeypadActivity::class.java,
        ).create().start().resume()
        val activity = activityController.get()
        val replacement = RecordingKeypadWindowCallback(activity)
        activity.window.callback = replacement

        // Act
        testSubject.onActivityDestroyed(activity)

        // Assert
        assertThat(activity.window.callback).isSameInstanceAs(replacement)
    }
}
