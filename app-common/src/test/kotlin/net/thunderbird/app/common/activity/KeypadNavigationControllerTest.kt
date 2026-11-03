package net.thunderbird.app.common.activity

import android.view.KeyEvent
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import net.thunderbird.core.android.testing.RobolectricTest
import org.junit.Test
import org.robolectric.annotation.Config

@Config(sdk = [26, 31, 36])
class KeypadNavigationControllerTest : RobolectricTest() {
    private val testSubject = KeypadNavigationController()
    private val actions = mutableListOf<KeypadNavigationAction>()

    @Test
    fun `disabled mode should leave all keypad shortcuts unhandled`() {
        // Arrange
        val keys = listOf(KeyEvent.KEYCODE_F1, KeyEvent.KEYCODE_F2, KeyEvent.KEYCODE_STAR, KeyEvent.KEYCODE_POUND)

        // Act and Assert
        for (keyCode in keys) {
            assertThat(dispatch(event(KeyEvent.ACTION_DOWN, keyCode), enabled = false)).isFalse()
            assertThat(dispatch(event(KeyEvent.ACTION_UP, keyCode), enabled = false)).isFalse()
        }
        assertThat(actions).isEmpty()
    }

    @Test
    fun `soft keys should execute their actions once on release`() {
        // Arrange
        val keys = listOf(
            KeyEvent.KEYCODE_SOFT_LEFT,
            KeyEvent.KEYCODE_F1,
            KeyEvent.KEYCODE_SOFT_RIGHT,
            KeyEvent.KEYCODE_F2,
        )

        // Act
        for (keyCode in keys) {
            assertThat(dispatch(event(KeyEvent.ACTION_DOWN, keyCode))).isTrue()
            assertThat(dispatch(event(KeyEvent.ACTION_UP, keyCode))).isTrue()
            assertThat(dispatch(event(KeyEvent.ACTION_UP, keyCode))).isFalse()
        }

        // Assert
        assertThat(actions).containsExactly(
            KeypadNavigationAction.OPEN_MENU,
            KeypadNavigationAction.OPEN_MENU,
            KeypadNavigationAction.GO_BACK,
            KeypadNavigationAction.GO_BACK,
        )
    }

    @Test
    fun `star and pound should navigate in opposite directions outside text editors`() {
        // Act
        dispatch(event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_STAR))
        dispatch(event(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_STAR))
        dispatch(event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_POUND))
        dispatch(event(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_POUND))

        // Assert
        assertThat(actions).containsExactly(
            KeypadNavigationAction.PREVIOUS_CONTROL,
            KeypadNavigationAction.NEXT_CONTROL,
        )
    }

    @Test
    fun `text editors should retain star and pound characters`() {
        // Arrange
        val keys = listOf(KeyEvent.KEYCODE_STAR, KeyEvent.KEYCODE_POUND)

        // Act and Assert
        for (keyCode in keys) {
            assertThat(dispatch(event(KeyEvent.ACTION_DOWN, keyCode), textInputFocused = true)).isFalse()
            assertThat(dispatch(event(KeyEvent.ACTION_UP, keyCode), textInputFocused = true)).isFalse()
        }
        assertThat(actions).isEmpty()
    }

    @Test
    fun `numeric input and native navigation should remain unhandled`() {
        // Arrange
        val keys = (KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9) + listOf(
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_DEL,
            KeyEvent.KEYCODE_TAB,
        )

        // Act and Assert
        for (keyCode in keys) {
            assertThat(dispatch(event(KeyEvent.ACTION_DOWN, keyCode))).isFalse()
            assertThat(dispatch(event(KeyEvent.ACTION_UP, keyCode))).isFalse()
        }
        assertThat(actions).isEmpty()
    }

    @Test
    fun `modified keypad shortcuts should remain unhandled`() {
        // Arrange
        val modifiers = listOf(KeyEvent.META_SHIFT_ON, KeyEvent.META_CTRL_ON, KeyEvent.META_ALT_ON)

        // Act and Assert
        for (metaState in modifiers) {
            assertThat(dispatch(event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F1, metaState = metaState))).isFalse()
            assertThat(dispatch(event(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_F1, metaState = metaState))).isFalse()
        }
        assertThat(actions).isEmpty()
    }

    @Test
    fun `holding a shortcut should consume repeats and execute only once`() {
        // Act
        assertThat(dispatch(event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F1))).isTrue()
        assertThat(actions).isEmpty()
        assertThat(dispatch(event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F1, repeatCount = 1))).isTrue()
        assertThat(dispatch(event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F1, repeatCount = 2))).isTrue()
        assertThat(dispatch(event(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_F1))).isTrue()

        // Assert
        assertThat(actions).containsExactly(KeypadNavigationAction.OPEN_MENU)
    }

    @Test
    fun `repeat without an initial press should not start an action`() {
        // Act
        val downHandled = dispatch(event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F1, repeatCount = 1))
        val upHandled = dispatch(event(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_F1))

        // Assert
        assertThat(downHandled).isFalse()
        assertThat(upHandled).isFalse()
        assertThat(actions).isEmpty()
    }

    @Test
    fun `canceled release should consume the pending key without executing`() {
        // Arrange
        dispatch(event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F2))

        // Act
        val handled = dispatch(event(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_F2, flags = KeyEvent.FLAG_CANCELED))

        // Assert
        assertThat(handled).isTrue()
        assertThat(actions).isEmpty()
        assertThat(dispatch(event(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_F2))).isFalse()
    }

    @Test
    fun `release from another keyboard should not execute or consume the pending action`() {
        // Arrange
        dispatch(event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F1, deviceId = 1))

        // Act
        val otherKeyboardHandled = dispatch(event(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_F1, deviceId = 2))

        // Assert
        assertThat(otherKeyboardHandled).isFalse()
        assertThat(actions).isEmpty()
        assertThat(dispatch(event(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_F1, deviceId = 1))).isTrue()
        assertThat(actions).containsExactly(KeypadNavigationAction.OPEN_MENU)
    }

    @Test
    fun `disabling mode before release should swallow the pair without executing`() {
        // Arrange
        dispatch(event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F1))

        // Act
        val handled = dispatch(event(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_F1), enabled = false)

        // Assert
        assertThat(handled).isTrue()
        assertThat(actions).isEmpty()
        assertThat(dispatch(event(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_F1))).isFalse()
    }

    @Test
    fun `enabling mode after key down should not turn its release into a shortcut`() {
        // Arrange
        dispatch(event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F1), enabled = false)

        // Act
        val handled = dispatch(event(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_F1))

        // Assert
        assertThat(handled).isFalse()
        assertThat(actions).isEmpty()
    }

    @Test
    fun `modifier added before release should cancel the pending action`() {
        // Arrange
        dispatch(event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F1))

        // Act
        val handled = dispatch(event(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_F1, metaState = KeyEvent.META_SHIFT_ON))

        // Assert
        assertThat(handled).isTrue()
        assertThat(actions).isEmpty()
    }

    @Test
    fun `reset should discard pending actions and allow the next complete press`() {
        // Arrange
        dispatch(event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F1))

        // Act
        testSubject.reset()
        val staleReleaseHandled = dispatch(event(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_F1))
        dispatch(event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F1))
        dispatch(event(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_F1))

        // Assert
        assertThat(staleReleaseHandled).isFalse()
        assertThat(actions).containsExactly(KeypadNavigationAction.OPEN_MENU)
    }

    private fun dispatch(event: KeyEvent, enabled: Boolean = true, textInputFocused: Boolean = false) =
        testSubject.dispatchKeyEvent(event, enabled, textInputFocused, actions::add)

    private fun event(
        action: Int,
        keyCode: Int,
        deviceId: Int = 1,
        repeatCount: Int = 0,
        metaState: Int = 0,
        flags: Int = 0,
    ) = KeyEvent(0L, 0L, action, keyCode, repeatCount, metaState, deviceId, 0, flags)
}
