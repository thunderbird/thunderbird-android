package net.thunderbird.core.android.common.keypad

import android.app.Activity
import android.app.Dialog
import android.os.Looper
import android.view.Window
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 31, 36])
class KeypadDialogNavigationTest {
    private val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    private val installer = RecordingWindowInstaller()

    @Test
    fun `cached dialog should install on every attachment and release on dismissal`() {
        // Arrange
        val testSubject = KeypadDialogNavigation.bind(Dialog(activity), installer)

        // Act
        testSubject.show()
        shadowOf(Looper.getMainLooper()).idle()

        // Assert
        assertThat(installer.installedWindows).containsExactly(requireNotNull(testSubject.window))
        assertThat(installer.disposalCount).isEqualTo(0)

        // Act
        testSubject.dismiss()
        shadowOf(Looper.getMainLooper()).idle()

        // Assert
        assertThat(installer.disposalCount).isEqualTo(1)

        // Act
        testSubject.show()
        shadowOf(Looper.getMainLooper()).idle()

        // Assert
        assertThat(installer.installedWindows.size).isEqualTo(2)

        // Act
        testSubject.dismiss()
        shadowOf(Looper.getMainLooper()).idle()

        // Assert
        assertThat(installer.disposalCount).isEqualTo(2)
    }

    @Test
    fun `binding should preserve existing show and dismiss listeners`() {
        // Arrange
        val dialog = Dialog(activity)
        var showCount = 0
        var dismissCount = 0
        dialog.setOnShowListener { showCount++ }
        dialog.setOnDismissListener { dismissCount++ }
        val testSubject = KeypadDialogNavigation.bind(dialog, installer)

        // Act
        testSubject.show()
        shadowOf(Looper.getMainLooper()).idle()
        testSubject.dismiss()
        shadowOf(Looper.getMainLooper()).idle()

        // Assert
        assertThat(showCount).isEqualTo(1)
        assertThat(dismissCount).isEqualTo(1)
        assertThat(installer.disposalCount).isEqualTo(1)
    }

    private class RecordingWindowInstaller : KeypadWindowInstaller {
        val installedWindows = mutableListOf<Window>()
        var disposalCount = 0

        override fun install(window: Window): AutoCloseable {
            installedWindows.add(window)
            return AutoCloseable { disposalCount++ }
        }
    }
}
