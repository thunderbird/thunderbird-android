package net.thunderbird.app.common.activity

import android.view.InputDevice
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isSameInstanceAs
import assertk.assertions.isTrue
import net.thunderbird.core.android.testing.RobolectricTest
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

@Config(sdk = [26, 31, 36])
class KeypadWindowCallbackTest : RobolectricTest() {
    private val activity = Robolectric.buildActivity(RecordingKeypadActivity::class.java).setup().visible().get()
    private val interactionPreferences = FakeKeypadInteractionPreferences()
    private val delegate = RecordingKeypadWindowCallback(activity)
    private val testSubject = KeypadWindowCallback(activity, delegate, interactionPreferences)

    @Test
    fun `disabled mode should forward original events and return the delegate result`() {
        // Arrange
        interactionPreferences.save(interactionPreferences.getConfig().copy(isKeypadNavigationEnabled = false))
        delegate.handlesKeyEvents = true
        val down = keypadEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F1)
        val up = keypadEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_F1)

        // Act
        val downHandled = testSubject.dispatchKeyEvent(down)
        val upHandled = testSubject.dispatchKeyEvent(up)

        // Assert
        assertThat(downHandled).isTrue()
        assertThat(upHandled).isTrue()
        assertThat(delegate.events).containsExactly(down, up)
        assertThat(delegate.events[0]).isSameInstanceAs(down)
        assertThat(delegate.events[1]).isSameInstanceAs(up)
        assertThat(activity.menuOpenCount).isEqualTo(0)
    }

    @Test
    fun `F1 should open the menu once without forwarding the shortcut`() {
        // Act
        testSubject.dispatchKeyEvent(keypadEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F1))
        testSubject.dispatchKeyEvent(KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F1, 1))
        testSubject.dispatchKeyEvent(keypadEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_F1))

        // Assert
        assertThat(activity.menuOpenCount).isEqualTo(1)
        assertThat(delegate.events).isEmpty()
    }

    @Test
    fun `F2 should dispatch one back key pair for a platform activity`() {
        // Act
        testSubject.dispatchKeyEvent(keypadEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F2))
        testSubject.dispatchKeyEvent(keypadEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_F2))

        // Assert
        assertThat(delegate.events.map { it.action to it.keyCode }).containsExactly(
            KeyEvent.ACTION_DOWN to KeyEvent.KEYCODE_BACK,
            KeyEvent.ACTION_UP to KeyEvent.KEYCODE_BACK,
        )
    }

    @Test
    fun `F2 should use the back dispatcher when the activity owns one`() {
        // Arrange
        val backActivity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val backDelegate = RecordingKeypadWindowCallback(backActivity)
        var backPressCount = 0
        backActivity.onBackPressedDispatcher.addCallback(
            backActivity,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    backPressCount++
                }
            },
        )
        val testSubject = KeypadWindowCallback(backActivity, backDelegate, interactionPreferences)

        // Act
        testSubject.dispatchKeyEvent(keypadEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F2))
        testSubject.dispatchKeyEvent(keypadEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_F2))

        // Assert
        assertThat(backPressCount).isEqualTo(1)
        assertThat(backDelegate.events).isEmpty()
    }

    @Test
    fun `star and pound should dispatch complete reverse and forward tab pairs`() {
        // Arrange
        val source = InputDevice.SOURCE_KEYBOARD
        val down = KeyEvent(10L, 20L, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_STAR, 0, 0, 7, 0, 0, source)
        val up = KeyEvent(10L, 30L, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_STAR, 0, 0, 7, 0, 0, source)

        // Act
        testSubject.dispatchKeyEvent(down)
        testSubject.dispatchKeyEvent(up)
        testSubject.dispatchKeyEvent(keypadEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_POUND))
        testSubject.dispatchKeyEvent(keypadEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_POUND))

        // Assert
        assertThat(delegate.events.map { Triple(it.action, it.keyCode, it.metaState) }).containsExactly(
            Triple(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_TAB, KeyEvent.META_SHIFT_ON),
            Triple(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_TAB, KeyEvent.META_SHIFT_ON),
            Triple(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_TAB, 0),
            Triple(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_TAB, 0),
        )
        assertThat(delegate.events.take(2).map { it.deviceId }).containsExactly(7, 7)
        assertThat(delegate.events.take(2).map { it.source }).containsExactly(source, source)
    }

    @Test
    fun `focused EditText should retain original character events`() {
        // Arrange
        val editor = EditText(activity)
        showAndFocus(editor)
        assertThat(activity.currentFocus).isSameInstanceAs(editor)
        val events = listOf(KeyEvent.KEYCODE_STAR, KeyEvent.KEYCODE_POUND, KeyEvent.KEYCODE_3).flatMap { keyCode ->
            listOf(keypadEvent(KeyEvent.ACTION_DOWN, keyCode), keypadEvent(KeyEvent.ACTION_UP, keyCode))
        }

        // Act
        events.forEach { testSubject.dispatchKeyEvent(it) }

        // Assert
        assertThat(delegate.events).isEqualTo(events)
        events.forEachIndexed { index, event ->
            assertThat(delegate.events[index]).isSameInstanceAs(event)
        }
    }

    @Test
    fun `a focused non-editor should still allow keypad navigation`() {
        // Arrange
        val control = View(activity).apply { isFocusableInTouchMode = true }
        showAndFocus(control)
        assertThat(activity.currentFocus).isSameInstanceAs(control)

        // Act
        testSubject.dispatchKeyEvent(keypadEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_POUND))
        testSubject.dispatchKeyEvent(keypadEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_POUND))

        // Assert
        assertThat(delegate.events.map { it.keyCode }).containsExactly(KeyEvent.KEYCODE_TAB, KeyEvent.KEYCODE_TAB)
    }

    @Test
    fun `star and pound should move real View focus when the callback does not consume Tab`() {
        // Arrange
        val first = Button(activity).apply {
            text = "First"
            isFocusableInTouchMode = true
        }
        val second = Button(activity).apply {
            text = "Second"
            isFocusableInTouchMode = true
        }
        val controls = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(first)
            addView(second)
        }
        showAndFocus(controls)
        assertThat(first.requestFocus()).isTrue()

        // Act and assert
        testSubject.dispatchKeyEvent(keypadEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_POUND))
        testSubject.dispatchKeyEvent(keypadEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_POUND))
        assertThat(activity.currentFocus).isSameInstanceAs(second)

        testSubject.dispatchKeyEvent(keypadEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_STAR))
        testSubject.dispatchKeyEvent(keypadEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_STAR))
        assertThat(activity.currentFocus).isSameInstanceAs(first)
    }

    @Test
    fun `modified shortcuts and number keys should be forwarded without rewriting`() {
        // Arrange
        val events = listOf(
            KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F1, 0, KeyEvent.META_ALT_ON),
            KeyEvent(0L, 0L, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_F1, 0, KeyEvent.META_ALT_ON),
            keypadEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_7),
            keypadEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_7),
        )

        // Act
        events.forEach { testSubject.dispatchKeyEvent(it) }

        // Assert
        assertThat(delegate.events).isEqualTo(events)
        assertThat(activity.menuOpenCount).isEqualTo(0)
    }

    @Test
    fun `losing window focus should cancel the pending action and forward the focus change`() {
        // Arrange
        testSubject.dispatchKeyEvent(keypadEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F1))

        // Act
        testSubject.onWindowFocusChanged(false)
        testSubject.onWindowFocusChanged(true)
        val up = keypadEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_F1)
        testSubject.dispatchKeyEvent(up)

        // Assert
        assertThat(activity.menuOpenCount).isEqualTo(0)
        assertThat(delegate.windowFocusChanges).containsExactly(false, true)
        assertThat(delegate.events).containsExactly(up)
    }

    private fun showAndFocus(view: View) {
        activity.setContentView(view)
        val decorView = activity.window.decorView
        decorView.measure(
            View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY),
        )
        decorView.layout(0, 0, 480, 800)
        decorView.dispatchWindowFocusChanged(true)
        view.isFocusableInTouchMode = true
        assertThat(view.requestFocus()).isTrue()
    }
}
