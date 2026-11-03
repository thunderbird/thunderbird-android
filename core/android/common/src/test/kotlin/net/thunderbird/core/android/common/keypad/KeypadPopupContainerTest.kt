package net.thunderbird.core.android.common.keypad

import android.app.Activity
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isSameInstanceAs
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 31, 36])
class KeypadPopupContainerTest {
    private val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    private var enabled = true
    private var dismissCount = 0
    private val testSubject = KeypadPopupContainer(activity, { enabled }, { dismissCount++ })

    @Test
    fun `star and pound should move real focus within the popup`() {
        // Arrange
        val first = Button(activity).apply {
            text = "First"
            isFocusableInTouchMode = true
        }
        val second = Button(activity).apply {
            text = "Second"
            isFocusableInTouchMode = true
        }
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(first)
            addView(second)
        }
        showContent(content)
        first.requestFocus()

        // Act and Assert
        press(KeyEvent.KEYCODE_POUND)
        assertThat(testSubject.findFocus()).isSameInstanceAs(second)
        press(KeyEvent.KEYCODE_STAR)
        assertThat(testSubject.findFocus()).isSameInstanceAs(first)
    }

    @Test
    fun `holding right soft key should dismiss only once on release`() {
        // Act
        testSubject.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F2))
        assertThat(dismissCount).isEqualTo(0)
        testSubject.dispatchKeyEvent(KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F2, 1))
        testSubject.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_F2))

        // Assert
        assertThat(dismissCount).isEqualTo(1)
    }

    @Test
    fun `disabled or modified soft keys should retain normal handling`() {
        // Arrange
        enabled = false

        // Act
        press(KeyEvent.KEYCODE_F2)
        enabled = true
        testSubject.dispatchKeyEvent(
            KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F2, 0, KeyEvent.META_CTRL_ON),
        )
        testSubject.dispatchKeyEvent(
            KeyEvent(0L, 0L, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_F2, 0, KeyEvent.META_CTRL_ON),
        )

        // Assert
        assertThat(dismissCount).isEqualTo(0)
    }

    @Test
    fun `text input should retain star pound and digits`() {
        // Arrange
        val editor = EditText(activity).apply { isFocusableInTouchMode = true }
        showContent(editor)
        editor.requestFocus()

        // Act
        press(KeyEvent.KEYCODE_STAR)
        press(KeyEvent.KEYCODE_POUND)
        press(KeyEvent.KEYCODE_3)

        // Assert
        assertThat(editor.text.toString()).isEqualTo("*#3")
        assertThat(testSubject.findFocus()).isSameInstanceAs(editor)
    }

    @Test
    fun `focus loss should cancel an unfinished dismiss shortcut`() {
        // Arrange
        testSubject.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F2))

        // Act
        testSubject.onWindowFocusChanged(false)
        testSubject.onWindowFocusChanged(true)
        testSubject.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_F2))

        // Assert
        assertThat(dismissCount).isEqualTo(0)
    }

    @Test
    fun `disabling navigation before release should cancel the popup shortcut`() {
        // Arrange
        testSubject.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F2))

        // Act
        enabled = false
        testSubject.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_F2))

        // Assert
        assertThat(dismissCount).isEqualTo(0)
    }

    @Test
    fun `release from another input device should not dismiss the popup`() {
        // Arrange
        testSubject.dispatchKeyEvent(KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F2, 0, 0, 1, 0))

        // Act
        val otherReleaseHandled = testSubject.dispatchKeyEvent(
            KeyEvent(0L, 0L, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_F2, 0, 0, 2, 0),
        )

        // Assert
        assertThat(otherReleaseHandled).isFalse()
        assertThat(dismissCount).isEqualTo(0)

        // Act
        testSubject.dispatchKeyEvent(KeyEvent(0L, 0L, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_F2, 0, 0, 1, 0))

        // Assert
        assertThat(dismissCount).isEqualTo(1)
    }

    private fun press(keyCode: Int) {
        testSubject.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        testSubject.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    private fun showContent(view: View) {
        testSubject.addView(view)
        activity.setContentView(testSubject)
        val decorView = activity.window.decorView
        decorView.measure(
            View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY),
        )
        decorView.layout(0, 0, 480, 800)
        decorView.dispatchWindowFocusChanged(true)
    }
}
