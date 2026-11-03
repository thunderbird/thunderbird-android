package com.fsck.k9.activity.compose

import android.os.Looper
import android.view.KeyEvent
import android.view.Menu
import android.view.ViewConfiguration
import android.widget.EditText
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.fsck.k9.ui.R
import java.util.concurrent.TimeUnit
import net.thunderbird.core.android.common.keypad.KeypadWindowInstaller
import net.thunderbird.core.android.testing.RobolectricTest
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowViewRootImpl
import org.robolectric.util.ReflectionHelpers

@Config(sdk = [26, 31, 36])
class ComposeAttachmentMenuTest : RobolectricTest() {
    private lateinit var activity: AppCompatActivity
    private lateinit var menu: Menu
    private val invokedActions = mutableListOf<Int>()
    private var disposalCount = 0
    private val installer = KeypadWindowInstaller { AutoCloseable { disposalCount++ } }

    @Before
    fun setUp() {
        val controller = Robolectric.buildActivity(AppCompatActivity::class.java)
        activity = controller.get()
        activity.setTheme(R.style.Theme_Legacy_Test)
        controller.setup()
        val anchor = EditText(activity)
        activity.setContentView(anchor)
        menu = PopupMenu(activity, anchor).menu
        for (id in listOf(R.id.attach_file, R.id.open_camera)) {
            menu.add(0, id, 0, id.toString()).setOnMenuItemClickListener {
                invokedActions.add(it.itemId)
                true
            }
        }
    }

    @Test
    fun `attachment list should close once on soft-right release without invoking an attachment action`() {
        // Arrange
        val testSubject = showMenu()
        val list = testSubject.listView

        // Act
        list.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SOFT_RIGHT))
        list.dispatchKeyEvent(KeyEvent(0L, 1L, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SOFT_RIGHT, 1))
        val showingBeforeRelease = testSubject.isShowing
        list.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SOFT_RIGHT))
        list.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SOFT_RIGHT))
        shadowOf(Looper.getMainLooper()).idle()

        // Assert
        assertThat(showingBeforeRelease).isTrue()
        assertThat(testSubject.isShowing).isFalse()
        assertThat(disposalCount).isEqualTo(1)
        assertThat(invokedActions).isEmpty()
    }

    @Test
    fun `cancelled F2 release should retain the list and center should still invoke the existing menu action`() {
        // Arrange
        val testSubject = showMenu()
        val list = testSubject.listView
        list.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F2))

        // Act
        list.dispatchKeyEvent(
            KeyEvent(0L, 1L, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_F2, 0, 0, -1, 0, KeyEvent.FLAG_CANCELED),
        )
        val showingAfterCancellation = testSubject.isShowing
        shadowOf(Looper.getMainLooper()).idleFor(
            ViewConfiguration.getPressedStateDuration().toLong(),
            TimeUnit.MILLISECONDS,
        )
        list.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER))
        list.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER))
        shadowOf(Looper.getMainLooper()).idleFor(
            ViewConfiguration.getPressedStateDuration().toLong(),
            TimeUnit.MILLISECONDS,
        )

        // Assert
        assertThat(showingAfterCancellation).isTrue()
        assertThat(invokedActions).containsExactly(R.id.attach_file)
        assertThat(testSubject.isShowing).isFalse()
        assertThat(disposalCount).isEqualTo(1)
    }

    @Test
    fun `ordinary Android back should retain its native dialog cancellation behavior`() {
        // Arrange
        val testSubject = showMenu()

        // Act
        testSubject.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK))
        testSubject.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK))
        shadowOf(Looper.getMainLooper()).idle()

        // Assert
        assertThat(testSubject.isShowing).isFalse()
        assertThat(invokedActions).isEmpty()
    }

    @Test
    fun `disabled mode should leave the native popup in charge without requesting a window installer`() {
        // Arrange
        val testSubject = ComposeKeypadNavigation(activity) { false }
        var installerRequested = false

        // Act
        val handled = testSubject.showAttachmentMenu(menu) {
            installerRequested = true
            installer
        }

        // Assert
        assertThat(handled).isFalse()
        assertThat(installerRequested).isFalse()
    }

    private fun showMenu(): AlertDialog = ComposeAttachmentMenu.show(activity, menu, installer) {
        true
    }.also { dialog ->
        shadowOf(Looper.getMainLooper()).idle()
        val decor = requireNotNull(dialog.window).decorView
        val viewRoot = ReflectionHelpers.callInstanceMethod<Any>(decor, "getViewRootImpl")
        Shadow.extract<ShadowViewRootImpl>(viewRoot).callWindowFocusChanged(true)
        shadowOf(Looper.getMainLooper()).idleFor(
            ViewConfiguration.getPressedStateDuration().toLong(),
            TimeUnit.MILLISECONDS,
        )
        assertThat(decor.hasWindowFocus()).isTrue()
    }
}
