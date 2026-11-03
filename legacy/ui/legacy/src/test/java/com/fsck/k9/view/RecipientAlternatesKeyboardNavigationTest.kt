package com.fsck.k9.view

import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ListPopupWindow
import androidx.appcompat.app.AppCompatActivity
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isGreaterThan
import assertk.assertions.isTrue
import com.fsck.k9.ui.R
import java.util.concurrent.TimeUnit
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
class RecipientAlternatesKeyboardNavigationTest : RobolectricTest() {
    private lateinit var activity: AppCompatActivity
    private lateinit var popup: ListPopupWindow
    private lateinit var testSubject: RecipientAlternatesKeyboardNavigation
    private val selectedPositions = mutableListOf<Int>()
    private var enabled = true

    @Before
    fun setUp() {
        val controller = Robolectric.buildActivity(AppCompatActivity::class.java)
        activity = controller.get()
        activity.setTheme(R.style.Theme_Legacy_Test)
        controller.setup()
        val anchor = EditText(activity).apply { setText("Original recipient input") }
        activity.setContentView(
            FrameLayout(activity).apply {
                addView(
                    anchor,
                    FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
                )
            },
        )
        activity.window.decorView.apply {
            measure(
                View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.EXACTLY),
            )
            layout(0, 0, 480, 640)
            dispatchWindowFocusChanged(true)
        }
        popup = ListPopupWindow(activity).apply {
            anchorView = anchor
            animationStyle = 0
            width = 400
            height = ViewGroup.LayoutParams.WRAP_CONTENT
            setAdapter(
                ArrayAdapter(
                    activity,
                    android.R.layout.simple_list_item_1,
                    listOf("first@example.com", "second@example.com"),
                ),
            )
            setOnItemClickListener { _, _, position, _ -> selectedPositions.add(position) }
        }
        testSubject = RecipientAlternatesKeyboardNavigation(popup) { enabled }
    }

    @Test
    fun `popup should receive keyboard focus and direction keys should select an alternate before confirming`() {
        // Arrange
        showPopup()

        // Act
        testSubject.onKeyDown(KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN))
        shadowOf(Looper.getMainLooper()).idleFor(
            ViewConfiguration.getPressedStateDuration().toLong(),
            TimeUnit.MILLISECONDS,
        )
        val selected = popup.selectedItemPosition
        testSubject.onKeyDown(
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER),
        )
        testSubject.onKeyUp(KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER))
        shadowOf(Looper.getMainLooper()).idleFor(
            ViewConfiguration.getPressedStateDuration().toLong(),
            TimeUnit.MILLISECONDS,
        )

        // Assert
        assertThat(popup.isModal).isTrue()
        assertThat(selected).isEqualTo(1)
        assertThat(selectedPositions).containsExactly(1)
        assertThat(popup.isShowing).isFalse()
    }

    @Test
    fun `cancelled confirmation must not select an alternate or dismiss the popup`() {
        // Arrange
        showPopup()
        testSubject.onKeyDown(KeyEvent.KEYCODE_ENTER, KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))

        // Act
        val handled = testSubject.onKeyUp(
            KeyEvent.KEYCODE_ENTER,
            KeyEvent(0L, 1L, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER, 0, 0, -1, 0, KeyEvent.FLAG_CANCELED),
        )

        // Assert
        assertThat(handled).isTrue()
        assertThat(selectedPositions).isEmpty()
        assertThat(popup.isShowing).isTrue()
    }

    @Test
    fun `back should close only the alternate popup on release and keep the composing activity open`() {
        // Arrange
        showPopup()

        // Act
        testSubject.onKeyPreIme(KeyEvent.KEYCODE_BACK, KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK))
        val openWhilePressed = popup.isShowing
        testSubject.onKeyPreIme(KeyEvent.KEYCODE_BACK, KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK))

        // Assert
        assertThat(openWhilePressed).isTrue()
        assertThat(popup.isShowing).isFalse()
        assertThat(activity.isFinishing).isFalse()
        assertThat(selectedPositions).isEmpty()
    }

    @Test
    fun `disabled mode and ordinary character keys should retain the existing editor handling`() {
        // Arrange
        enabled = false
        showPopup()

        // Act
        val disabledHandled = testSubject.onKeyDown(
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN),
        )
        enabled = true
        val characterHandled = testSubject.onKeyDown(
            KeyEvent.KEYCODE_POUND,
            KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_POUND),
        )

        // Assert
        assertThat(disabledHandled).isFalse()
        assertThat(characterHandled).isFalse()
        assertThat(popup.isModal).isFalse()
        assertThat(selectedPositions).isEmpty()
    }

    @Test
    fun `popup list should close once on a matching F2 or soft-right release without selecting an address`() {
        // Arrange
        var dismissCount = 0
        popup.setOnDismissListener { dismissCount++ }

        // Act and assert
        for (keyCode in listOf(KeyEvent.KEYCODE_F2, KeyEvent.KEYCODE_SOFT_RIGHT)) {
            showPopup()
            val list = requireNotNull(popup.listView)
            assertThat(list.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode)), "down keyCode=$keyCode").isTrue()
            list.dispatchKeyEvent(KeyEvent(0L, 1L, KeyEvent.ACTION_DOWN, keyCode, 2))
            assertThat(popup.isShowing).isTrue()
            assertThat(list.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))).isTrue()
            list.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
            shadowOf(Looper.getMainLooper()).idleFor(
                ViewConfiguration.getPressedStateDuration().toLong(),
                TimeUnit.MILLISECONDS,
            )
            assertThat(popup.isShowing).isFalse()
        }
        assertThat(dismissCount).isEqualTo(2)
        assertThat(selectedPositions).isEmpty()
    }

    @Test
    fun `popup list should ignore a cancelled soft-right release`() {
        // Arrange
        showPopup()
        val list = requireNotNull(popup.listView)
        list.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F2))

        // Act
        list.dispatchKeyEvent(
            KeyEvent(0L, 1L, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_F2, 0, 0, -1, 0, KeyEvent.FLAG_CANCELED),
        )

        // Assert
        assertThat(popup.isShowing).isTrue()
        assertThat(selectedPositions).isEmpty()
    }

    @Test
    fun `initial keyboard selection should skip a disabled header`() {
        // Arrange
        popup.setAdapter(
            object : ArrayAdapter<String>(
                activity,
                android.R.layout.simple_list_item_1,
                listOf("Header", "first@example.com", "second@example.com"),
            ) {
                override fun areAllItemsEnabled(): Boolean = false
                override fun isEnabled(position: Int): Boolean = position != 0
            },
        )

        // Act
        showPopup()

        // Assert
        assertThat(popup.selectedItemPosition).isEqualTo(1)
    }

    private fun showPopup() {
        testSubject.prepareToShow()
        popup.show()
        shadowOf(Looper.getMainLooper()).idle()
        val list = requireNotNull(popup.listView)
        if (popup.isModal) {
            val viewRoot = ReflectionHelpers.callInstanceMethod<Any>(list.rootView, "getViewRootImpl")
            Shadow.extract<ShadowViewRootImpl>(viewRoot).callWindowFocusChanged(true)
            list.requestFocusFromTouch()
            shadowOf(Looper.getMainLooper()).idle()
            assertThat(list.hasWindowFocus()).isTrue()
        }
        testSubject.onPopupShown()
        shadowOf(Looper.getMainLooper()).idleFor(
            ViewConfiguration.getPressedStateDuration().toLong(),
            TimeUnit.MILLISECONDS,
        )
        assertThat(list.height).isGreaterThan(0)
        if (popup.isModal) assertThat(list.isFocused).isTrue()
    }
}
