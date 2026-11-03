package net.thunderbird.app.common.activity

import android.app.Dialog
import android.view.KeyEvent
import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.isSameInstanceAs
import assertk.assertions.isTrue
import net.thunderbird.core.android.testing.RobolectricTest
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

@Config(sdk = [26, 31, 36])
class DefaultKeypadWindowInstallerTest : RobolectricTest() {
    private val activity = Robolectric.buildActivity(RecordingKeypadActivity::class.java).setup().get()
    private val dialog = Dialog(activity)
    private val window = requireNotNull(dialog.window)
    private val testSubject = DefaultKeypadWindowInstaller(FakeKeypadInteractionPreferences())

    @Test
    fun `duplicate installation should leave one wrapper and only its owner should restore the delegate`() {
        // Arrange
        val originalCallback = window.callback

        // Act
        val installation = testSubject.install(window)
        val callback = window.callback
        val duplicate = testSubject.install(window)
        duplicate.close()

        // Assert
        assertThat(callback).isInstanceOf<KeypadDialogWindowCallback>()
        assertThat(window.callback).isSameInstanceAs(callback)

        // Act
        installation.close()

        // Assert
        assertThat(window.callback).isSameInstanceAs(originalCallback)
    }

    @Test
    fun `disposing should leave an independently replaced callback intact`() {
        // Arrange
        val installation = testSubject.install(window)
        val replacement = RecordingKeypadWindowCallback(activity)
        window.callback = replacement

        // Act
        installation.close()

        // Assert
        assertThat(window.callback).isSameInstanceAs(replacement)
    }

    @Test
    fun `disposing should discard any unfinished dialog shortcut`() {
        // Arrange
        dialog.show()
        val installation = testSubject.install(window)
        val callback = requireNotNull(window.callback)
        callback.dispatchKeyEvent(keypadEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F2))

        // Act
        installation.close()
        callback.dispatchKeyEvent(keypadEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_F2))

        // Assert
        assertThat(dialog.isShowing).isTrue()
    }
}
