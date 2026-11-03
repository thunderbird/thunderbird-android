package net.thunderbird.app.common.activity

import android.app.Dialog
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isSameInstanceAs
import assertk.assertions.isTrue
import net.thunderbird.core.android.testing.RobolectricTest
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@Config(sdk = [26, 31, 36])
class KeypadDialogWindowCallbackTest : RobolectricTest() {
    private val activity = Robolectric.buildActivity(RecordingKeypadActivity::class.java).setup().get()
    private val dialog = FocusableKeypadDialog(activity)
    private val interactionPreferences = FakeKeypadInteractionPreferences()

    @Test
    fun `right soft key should cancel the dialog through its normal back handling`() {
        // Arrange
        var canceled = false
        dialog.setOnCancelListener { canceled = true }
        dialog.show()
        val testSubject = createCallback()

        // Act
        dispatchPress(testSubject, KeyEvent.KEYCODE_F2)

        // Assert
        assertThat(canceled).isTrue()
        assertThat(dialog.isShowing).isFalse()
    }

    @Test
    fun `right soft key should preserve a noncancelable dialog`() {
        // Arrange
        dialog.setCancelable(false)
        dialog.show()
        val testSubject = createCallback()

        // Act
        dispatchPress(testSubject, KeyEvent.KEYCODE_SOFT_RIGHT)

        // Assert
        assertThat(dialog.isShowing).isTrue()
    }

    @Test
    fun `disabled mode should not turn F2 into a dialog cancellation`() {
        // Arrange
        interactionPreferences.save(interactionPreferences.getConfig().copy(isKeypadNavigationEnabled = false))
        dialog.show()
        val testSubject = createCallback()

        // Act
        dispatchPress(testSubject, KeyEvent.KEYCODE_F2)

        // Assert
        assertThat(dialog.isShowing).isTrue()
    }

    @Test
    fun `star and pound should move between real dialog controls`() {
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
        showAndLayout(content)
        first.requestFocus()
        val testSubject = createCallback()

        // Act and Assert
        dispatchPress(testSubject, KeyEvent.KEYCODE_POUND)
        assertThat(dialog.currentFocus).isSameInstanceAs(second)
        dispatchPress(testSubject, KeyEvent.KEYCODE_STAR)
        assertThat(dialog.currentFocus).isSameInstanceAs(first)
    }

    @Test
    fun `focused dialog text editor should keep star and pound available to the input method`() {
        // Arrange
        val editor = EditText(activity)
        showAndLayout(editor)
        editor.isFocusableInTouchMode = true
        editor.requestFocus()
        val testSubject = createCallback()

        // Act
        dispatchPress(testSubject, KeyEvent.KEYCODE_STAR)
        dispatchPress(testSubject, KeyEvent.KEYCODE_POUND)

        // Assert
        assertThat(editor.text.toString()).isEqualTo("*#")
        assertThat(dialog.currentFocus).isSameInstanceAs(editor)
    }

    @Test
    fun `F1 should move focus out of a real dialog text field without changing its contents`() {
        // Arrange
        val editor = EditText(activity).apply {
            setText("Server name")
            isFocusableInTouchMode = true
        }
        val button = Button(activity).apply {
            text = "Confirm"
            isFocusableInTouchMode = true
        }
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(editor)
            addView(button)
        }
        showAndLayout(content)
        editor.requestFocus()
        val testSubject = createCallback()

        // Act
        dispatchPress(testSubject, KeyEvent.KEYCODE_F1)

        // Assert
        assertThat(dialog.currentFocus).isSameInstanceAs(button)
        assertThat(editor.text.toString()).isEqualTo("Server name")
    }

    @Test
    fun `losing dialog focus should discard an unfinished back shortcut`() {
        // Arrange
        dialog.show()
        val testSubject = createCallback()
        testSubject.dispatchKeyEvent(keypadEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F2))

        // Act
        testSubject.onWindowFocusChanged(false)
        testSubject.onWindowFocusChanged(true)
        testSubject.dispatchKeyEvent(keypadEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_F2))

        // Assert
        assertThat(dialog.isShowing).isTrue()
    }

    private fun createCallback() = KeypadDialogWindowCallback(
        dialog,
        requireNotNull(dialog.window?.callback),
        interactionPreferences,
    )

    private fun dispatchPress(testSubject: KeypadDialogWindowCallback, keyCode: Int) {
        focusKeypadDialogWindow(dialog)
        testSubject.dispatchKeyEvent(keypadEvent(KeyEvent.ACTION_DOWN, keyCode))
        testSubject.dispatchKeyEvent(keypadEvent(KeyEvent.ACTION_UP, keyCode))
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun showAndLayout(view: View) {
        dialog.setContentView(view)
        dialog.show()
        val decorView = requireNotNull(dialog.window).decorView
        decorView.measure(
            View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY),
        )
        decorView.layout(0, 0, 480, 800)
        decorView.dispatchWindowFocusChanged(true)
    }
}

private class FocusableKeypadDialog(context: android.content.Context) : Dialog(context) {
    override fun getCurrentFocus(): View? = window?.decorView?.findFocus()
}
