package net.thunderbird.app.common.activity

import android.app.Activity
import android.os.Looper
import android.view.Menu
import androidx.appcompat.app.AlertDialog
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import net.thunderbird.app.common.R
import net.thunderbird.core.android.testing.RobolectricTest
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

@Config(sdk = [26, 31, 36])
class KeypadFallbackMenuTest : RobolectricTest() {
    private val activityController = Robolectric.buildActivity(Activity::class.java)
    private val activity = activityController.get().apply { setTheme(androidx.appcompat.R.style.Theme_AppCompat) }
    private val preferences = FakeKeypadInteractionPreferences(enabled = false)
    private val testSubject = KeypadFallbackMenu(activity)

    init {
        activityController.setup()
    }

    @Test
    fun `opening activation menu should leave mode disabled until Enable is explicitly selected`() {
        // Arrange
        val menu = testSubject.activation {
            preferences.save(preferences.getConfig().copy(isKeypadNavigationEnabled = true))
        }
        val navigator = KeypadMenuNavigator(activity, DefaultKeypadWindowInstaller(preferences))

        // Act
        navigator.showMenu(menu)
        val dialog = latestDialog()

        // Assert
        assertThat(preferences.getConfig().isKeypadNavigationEnabled).isFalse()

        // Act
        select(dialog, menu, R.string.keypad_enable)

        // Assert
        assertThat(preferences.getConfig().isKeypadNavigationEnabled).isTrue()
        assertThat(dialog.isShowing).isFalse()
    }

    @Test
    fun `selecting Cancel should close activation menu without changing the preference`() {
        // Arrange
        var enableCalled = false
        val menu = testSubject.activation {
            enableCalled = true
            preferences.save(preferences.getConfig().copy(isKeypadNavigationEnabled = true))
        }
        val navigator = KeypadMenuNavigator(activity, DefaultKeypadWindowInstaller(preferences))
        navigator.showMenu(menu)
        val dialog = latestDialog()

        // Act
        select(dialog, menu, android.R.string.cancel)
        shadowOf(Looper.getMainLooper()).idle()

        // Assert
        assertThat(enableCalled).isFalse()
        assertThat(preferences.getConfig().isKeypadNavigationEnabled).isFalse()
        assertThat(dialog.isShowing).isFalse()
    }

    @Test
    fun `dismissing activation menu without choosing an item should not enable keypad mode`() {
        // Arrange
        val menu = testSubject.activation {
            preferences.save(preferences.getConfig().copy(isKeypadNavigationEnabled = true))
        }
        val navigator = KeypadMenuNavigator(activity, DefaultKeypadWindowInstaller(preferences))
        navigator.showMenu(menu)

        // Act
        latestDialog().cancel()
        shadowOf(Looper.getMainLooper()).idle()

        // Assert
        assertThat(preferences.getConfig().isKeypadNavigationEnabled).isFalse()
    }

    @Test
    fun `navigation items should invoke only their corresponding callbacks`() {
        // Arrange
        val actions = mutableListOf<String>()
        val menu = testSubject.navigation(
            next = { actions.add("next") },
            previous = { actions.add("previous") },
            back = { actions.add("back") },
        )

        // Assert
        assertThat(actions).isEmpty()

        // Act and Assert
        menu.performIdentifierAction(itemId(menu, R.string.keypad_next_control), 0)
        assertThat(actions).containsExactly("next")
        menu.performIdentifierAction(itemId(menu, R.string.keypad_previous_control), 0)
        assertThat(actions).containsExactly("next", "previous")
        menu.performIdentifierAction(itemId(menu, R.string.keypad_back), 0)
        assertThat(actions).containsExactly("next", "previous", "back")
        assertThat(preferences.getConfig().isKeypadNavigationEnabled).isFalse()
    }

    private fun latestDialog() = ShadowDialog.getLatestDialog() as AlertDialog

    private fun itemId(menu: Menu, titleResource: Int): Int =
        (0 until menu.size()).map(menu::getItem).single { it.title == activity.getString(titleResource) }.itemId

    private fun select(dialog: AlertDialog, menu: Menu, titleResource: Int) {
        val id = itemId(menu, titleResource)
        val position = (0 until dialog.listView.adapter.count).single { index ->
            (dialog.listView.adapter.getItem(index) as android.view.MenuItem).itemId == id
        }
        dialog.listView.performItemClick(null, position, id.toLong())
    }
}
