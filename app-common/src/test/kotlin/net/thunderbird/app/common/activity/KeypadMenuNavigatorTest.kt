package net.thunderbird.app.common.activity

import android.app.Activity
import android.os.Looper
import android.view.KeyEvent
import android.view.Menu
import android.view.View
import android.view.ViewConfiguration
import android.widget.CheckedTextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.test.platform.app.InstrumentationRegistry
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import java.util.concurrent.TimeUnit
import net.thunderbird.core.android.common.keypad.KeypadWindowInstaller
import net.thunderbird.core.android.testing.RobolectricTest
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

@Config(sdk = [26, 31, 36])
class KeypadMenuNavigatorTest : RobolectricTest() {
    private val activityController = Robolectric.buildActivity(Activity::class.java)
    private val activity = activityController.get().apply {
        setTheme(androidx.appcompat.R.style.Theme_AppCompat)
        title = "Inbox"
    }
    private val menu: Menu
    private val installer = CountingWindowInstaller(DefaultKeypadWindowInstaller(FakeKeypadInteractionPreferences()))
    private val testSubject = KeypadMenuNavigator(activity, installer)

    init {
        activityController.setup()
        menu = Toolbar(activity).menu
    }

    @Test
    fun `preferred next-field item should receive confirmation without triggering Send or changing menu order`() {
        // Arrange
        val actions = mutableListOf<String>()
        menu.add(Menu.NONE, SEND_ID, 0, "Send").setOnMenuItemClickListener { actions.add("send") }
        menu.add(Menu.NONE, NEXT_ID, 1, "Next field").setOnMenuItemClickListener { actions.add("next") }
        menu.add(Menu.NONE, PREVIOUS_ID, 2, "Previous field")
        menu.add(Menu.NONE, HIDDEN_ID, 3, "Hidden").isVisible = false

        // Act
        val shown = testSubject.showMenu(menu, listOf(HIDDEN_ID, NEXT_ID, NEXT_ID, PREVIOUS_ID))
        val dialog = latestDialog()
        layoutDialog(dialog)

        // Assert
        assertThat(shown).isTrue()
        assertThat(dialog.listView.adapter.count).isEqualTo(3)
        assertThat((dialog.listView.adapter.getItem(0) as android.view.MenuItem).itemId).isEqualTo(NEXT_ID)
        assertThat((0 until menu.size()).map { menu.getItem(it).itemId }).containsExactly(
            SEND_ID,
            NEXT_ID,
            PREVIOUS_ID,
            HIDDEN_ID,
        )
        assertThat(actions).isEmpty()

        // Act
        press(dialog, KeyEvent.KEYCODE_DPAD_CENTER)

        // Assert
        assertThat(actions).containsExactly("next")
        assertThat(dialog.isShowing).isFalse()
    }

    @Test
    fun `menu receiving touch mode on attachment should activate the preferred item on the first center press`() {
        // Arrange
        val actions = mutableListOf<String>()
        menu.add(Menu.NONE, SEND_ID, 0, "Send").setOnMenuItemClickListener { actions.add("send") }
        menu.add(Menu.NONE, NEXT_ID, 1, "Next field").setOnMenuItemClickListener { actions.add("next") }
        val instrumentation = InstrumentationRegistry.getInstrumentation()

        try {
            // Act - model touch mode arriving with the new window's first layout/focus event.
            testSubject.showMenu(menu, preferredItemIds = listOf(NEXT_ID))
            instrumentation.setInTouchMode(true)
            val dialog = latestDialog()
            layoutDialog(dialog)

            // Assert
            assertThat(dialog.listView.isInTouchMode).isFalse()
            assertThat((dialog.listView.selectedItem as android.view.MenuItem).itemId).isEqualTo(NEXT_ID)

            // Act - no directional key is needed to establish the initial selection.
            press(dialog, KeyEvent.KEYCODE_DPAD_CENTER)

            // Assert
            assertThat(actions).containsExactly("next")
            assertThat(dialog.isShowing).isFalse()
        } finally {
            instrumentation.setInTouchMode(false)
        }
    }

    @Test
    fun `disabled items should be visible but never execute and checked state should be displayed`() {
        // Arrange
        var executed = false
        menu.add(Menu.NONE, NEXT_ID, 0, "Unavailable").apply {
            isEnabled = false
            setOnMenuItemClickListener {
                executed = true
                true
            }
        }
        menu.add(Menu.NONE, PREVIOUS_ID, 1, "Checked").apply {
            isCheckable = true
            isChecked = true
        }

        // Act
        testSubject.showMenu(menu)
        val dialog = latestDialog()
        val adapter = dialog.listView.adapter
        dialog.listView.performItemClick(null, 0, NEXT_ID.toLong())

        // Assert
        assertThat(adapter.count).isEqualTo(2)
        assertThat(adapter.isEnabled(0)).isFalse()
        assertThat(executed).isFalse()
        assertThat(dialog.isShowing).isTrue()
        val checkedRow = adapter.getView(1, null, dialog.listView) as CheckedTextView
        assertThat(checkedRow.isChecked).isTrue()
    }

    @Test
    fun `F2 in a submenu should return to its parent and F2 at the root should close the menu`() {
        // Arrange
        val submenu = menu.addSubMenu(Menu.NONE, NEXT_ID, 0, "Sort")
        submenu.add(Menu.NONE, PREVIOUS_ID, 0, "Newest first")
        testSubject.showMenu(menu)
        val rootDialog = latestDialog()

        // Act
        rootDialog.listView.performItemClick(null, 0, NEXT_ID.toLong())
        val submenuDialog = latestDialog()
        layoutDialog(submenuDialog)
        press(submenuDialog, KeyEvent.KEYCODE_F2)
        shadowOf(Looper.getMainLooper()).idle()
        val restoredParent = latestDialog()

        // Assert
        assertThat(rootDialog.isShowing).isFalse()
        assertThat(submenuDialog.isShowing).isFalse()
        assertThat(restoredParent.isShowing).isTrue()
        assertThat((restoredParent.listView.adapter.getItem(0) as android.view.MenuItem).title.toString())
            .isEqualTo("Sort")

        // Act
        layoutDialog(restoredParent)
        press(restoredParent, KeyEvent.KEYCODE_F2)
        shadowOf(Looper.getMainLooper()).idle()

        // Assert
        assertThat(restoredParent.isShowing).isFalse()
        assertThat(installer.disposalCount).isEqualTo(installer.installCount)
    }

    @Test
    fun `expandable action views should expand before invoking the original menu callback`() {
        // Arrange
        var callbackCount = 0
        val item = menu.add(Menu.NONE, NEXT_ID, 0, "Search").apply {
            actionView = View(activity)
            setShowAsAction(ACTION_VIEW_FLAGS)
            setOnMenuItemClickListener {
                callbackCount++
                true
            }
        }
        testSubject.showMenu(menu)
        val dialog = latestDialog()

        // Act
        dialog.listView.performItemClick(null, 0, NEXT_ID.toLong())

        // Assert
        assertThat(item.isActionViewExpanded).isTrue()
        assertThat(callbackCount).isEqualTo(0)
        assertThat(dialog.isShowing).isFalse()
    }

    @Test
    fun `close should dismiss the current menu and release its window installation exactly once`() {
        // Arrange
        menu.add(Menu.NONE, NEXT_ID, 0, "Refresh")
        testSubject.showMenu(menu)
        val dialog = latestDialog()

        // Act
        testSubject.close()
        testSubject.close()
        shadowOf(Looper.getMainLooper()).idle()

        // Assert
        assertThat(dialog.isShowing).isFalse()
        assertThat(installer.installCount).isEqualTo(1)
        assertThat(installer.disposalCount).isEqualTo(1)
    }

    @Test
    fun `menus with no visible items should not create a dialog`() {
        // Arrange
        menu.add("Hidden").isVisible = false

        // Act
        val shown = testSubject.showMenu(menu)

        // Assert
        assertThat(shown).isFalse()
        assertThat(installer.installCount).isEqualTo(0)
    }

    private fun latestDialog() = ShadowDialog.getLatestDialog() as AlertDialog

    private fun press(dialog: AlertDialog, keyCode: Int) {
        val callback = requireNotNull(dialog.window?.callback)
        callback.dispatchKeyEvent(keypadEvent(KeyEvent.ACTION_DOWN, keyCode))
        callback.dispatchKeyEvent(keypadEvent(KeyEvent.ACTION_UP, keyCode))
        shadowOf(Looper.getMainLooper()).idleFor(
            ViewConfiguration.getPressedStateDuration().toLong(),
            TimeUnit.MILLISECONDS,
        )
    }

    private fun layoutDialog(dialog: AlertDialog) {
        val decor = requireNotNull(dialog.window).decorView
        decor.measure(
            View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY),
        )
        decor.layout(0, 0, 480, 800)
        decor.dispatchWindowFocusChanged(true)
        focusKeypadDialogWindow(dialog)
    }

    private class CountingWindowInstaller(private val delegate: KeypadWindowInstaller) : KeypadWindowInstaller {
        var installCount = 0
        var disposalCount = 0

        override fun install(window: android.view.Window): AutoCloseable {
            installCount++
            val installation = delegate.install(window)
            return AutoCloseable {
                disposalCount++
                installation.close()
            }
        }
    }

    private companion object {
        const val SEND_ID = 1
        const val NEXT_ID = 2
        const val PREVIOUS_ID = 3
        const val HIDDEN_ID = 4
        const val ACTION_VIEW_FLAGS = android.view.MenuItem.SHOW_AS_ACTION_ALWAYS or
            android.view.MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW
    }
}
