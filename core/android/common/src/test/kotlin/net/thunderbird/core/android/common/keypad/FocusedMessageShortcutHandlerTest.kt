package net.thunderbird.core.android.common.keypad

import android.view.KeyEvent
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 31, 36])
class FocusedMessageShortcutHandlerTest {
    private var contextActive = true
    private var focusedMessage: String? = "message-1"
    private val shortcuts = mutableMapOf(
        KeyEvent.KEYCODE_0 to MessageAction.DELETE,
        KeyEvent.KEYCODE_9 to MessageAction.ARCHIVE,
    )
    private val performedActions = mutableListOf<Pair<MessageAction, String>>()
    private val testSubject = FocusedMessageShortcutHandler(
        getShortcut = shortcuts::get,
        getTarget = { focusedMessage.takeIf { contextActive } },
        performAction = { action, target -> performedActions.add(action to target) },
    )

    @Test
    fun `each number key should perform each configured action only on its matching release`() {
        // Arrange
        val expectedActions = mutableListOf<Pair<MessageAction, String>>()

        // Act
        for (keyCode in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9) {
            for (action in MessageAction.entries) {
                shortcuts[keyCode] = action
                assertThat(testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_DOWN, keyCode))).isTrue()
                assertThat(performedActions).containsExactly(*expectedActions.toTypedArray())
                assertThat(testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_UP, keyCode))).isTrue()
                expectedActions.add(action to "message-1")
            }
        }

        // Assert
        assertThat(performedActions).containsExactly(*expectedActions.toTypedArray())
    }

    @Test
    fun `unmapped numbers and absent focused message contexts should remain unhandled`() {
        // Arrange
        shortcuts.clear()

        // Act and assert
        for (keyCode in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9) {
            assertThat(testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_DOWN, keyCode))).isFalse()
            assertThat(testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_UP, keyCode))).isFalse()
            shortcuts[keyCode] = MessageAction.DELETE
        }
        focusedMessage = null
        for (keyCode in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9) {
            assertThat(testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_DOWN, keyCode))).isFalse()
            assertThat(testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_UP, keyCode))).isFalse()
        }
        focusedMessage = "message-1"
        contextActive = false
        for (keyCode in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9) {
            assertThat(testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_DOWN, keyCode))).isFalse()
            assertThat(testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_UP, keyCode))).isFalse()
        }
        assertThat(performedActions).isEmpty()
    }

    @Test
    fun `changing or losing the focused message before release should discard the action`() {
        // Arrange
        testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_0))

        // Act
        focusedMessage = "message-2"
        val changedTargetHandled = testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_0))
        testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_9))
        focusedMessage = null
        val absentTargetHandled = testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_9))

        // Assert
        assertThat(changedTargetHandled).isTrue()
        assertThat(absentTargetHandled).isTrue()
        assertThat(performedActions).isEmpty()
    }

    @Test
    fun `changing the mapping or leaving the message context before release should discard the action`() {
        // Arrange
        testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_0))

        // Act
        shortcuts[KeyEvent.KEYCODE_0] = MessageAction.TOGGLE_FLAGGED
        val changedActionHandled = testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_0))
        testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_9))
        shortcuts.remove(KeyEvent.KEYCODE_9)
        val removedActionHandled = testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_9))
        testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_0))
        contextActive = false
        val changedContextHandled = testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_0))

        // Assert
        assertThat(changedActionHandled).isTrue()
        assertThat(removedActionHandled).isTrue()
        assertThat(changedContextHandled).isTrue()
        assertThat(performedActions).isEmpty()
    }

    @Test
    fun `holding a number should act once while cancelled releases and orphan repeats should not act`() {
        // Arrange
        testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_0))

        // Act
        val repeatHandled = testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_0, repeat = 2))
        val duplicateDownHandled = testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_0))
        testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_0))
        val duplicateReleaseHandled = testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_0))
        testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_9))
        val cancelledReleaseHandled = testSubject.dispatchKeyEvent(
            event(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_9, flags = KeyEvent.FLAG_CANCELED),
        )
        val orphanRepeatHandled = testSubject.dispatchKeyEvent(
            event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_9, repeat = 1),
        )

        // Assert
        assertThat(repeatHandled).isTrue()
        assertThat(duplicateDownHandled).isTrue()
        assertThat(duplicateReleaseHandled).isFalse()
        assertThat(cancelledReleaseHandled).isTrue()
        assertThat(orphanRepeatHandled).isFalse()
        assertThat(performedActions).containsExactly(MessageAction.DELETE to "message-1")
    }

    @Test
    fun `releases must match both number key and input device`() {
        // Arrange
        testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_0, deviceId = 1))
        testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_9, deviceId = 2))

        // Act
        val wrongDeviceHandled = testSubject.dispatchKeyEvent(
            event(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_0, deviceId = 2),
        )
        val wrongKeyHandled = testSubject.dispatchKeyEvent(
            event(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_9, deviceId = 1),
        )
        val matchingNineHandled = testSubject.dispatchKeyEvent(
            event(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_9, deviceId = 2),
        )
        val matchingZeroHandled = testSubject.dispatchKeyEvent(
            event(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_0, deviceId = 1),
        )

        // Assert
        assertThat(wrongDeviceHandled).isFalse()
        assertThat(wrongKeyHandled).isFalse()
        assertThat(matchingNineHandled).isTrue()
        assertThat(matchingZeroHandled).isTrue()
        assertThat(performedActions).containsExactly(
            MessageAction.ARCHIVE to "message-1",
            MessageAction.DELETE to "message-1",
        )
    }

    @Test
    fun `modified numbers and cancelled presses should not execute message actions`() {
        // Arrange
        val modifiers = listOf(
            KeyEvent.META_SHIFT_ON,
            KeyEvent.META_CTRL_ON,
            KeyEvent.META_ALT_ON,
            KeyEvent.META_META_ON,
        )

        // Act and assert
        for (modifier in modifiers) {
            val down = event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_0, metaState = modifier)
            val up = event(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_0, metaState = modifier)
            assertThat(testSubject.dispatchKeyEvent(down)).isFalse()
            assertThat(testSubject.dispatchKeyEvent(up)).isFalse()
        }
        testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_0))
        val modifiedRelease = event(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_0, metaState = KeyEvent.META_CTRL_ON)
        val cancelledPress = event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_9, flags = KeyEvent.FLAG_CANCELED)
        assertThat(testSubject.dispatchKeyEvent(modifiedRelease)).isTrue()
        assertThat(testSubject.dispatchKeyEvent(cancelledPress)).isFalse()
        assertThat(testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_MULTIPLE, KeyEvent.KEYCODE_0))).isFalse()
        assertThat(performedActions).isEmpty()
    }

    @Test
    fun `directional phone and system keys must pass through even if a caller supplies a mapping`() {
        // Arrange
        val keys = listOf(
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_CALL,
            KeyEvent.KEYCODE_ENDCALL,
            KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_POWER,
            KeyEvent.KEYCODE_STAR,
            KeyEvent.KEYCODE_POUND,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_F1,
            KeyEvent.KEYCODE_F2,
            KeyEvent.KEYCODE_SOFT_LEFT,
            KeyEvent.KEYCODE_SOFT_RIGHT,
            KeyEvent.KEYCODE_DEL,
            KeyEvent.KEYCODE_NUMPAD_0,
        )
        keys.forEach { shortcuts[it] = MessageAction.DELETE }

        // Act and assert
        for (keyCode in keys) {
            assertThat(testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_DOWN, keyCode))).isFalse()
            assertThat(testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_UP, keyCode))).isFalse()
        }
        assertThat(performedActions).isEmpty()
    }

    @Test
    fun `reset should discard all unfinished actions and allow a fresh press`() {
        // Arrange
        testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_0, deviceId = 1))
        testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_9, deviceId = 2))

        // Act
        testSubject.reset()
        val zeroReleaseHandled = testSubject.dispatchKeyEvent(
            event(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_0, deviceId = 1),
        )
        val nineReleaseHandled = testSubject.dispatchKeyEvent(
            event(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_9, deviceId = 2),
        )
        testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_0))
        testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_0))

        // Assert
        assertThat(zeroReleaseHandled).isFalse()
        assertThat(nineReleaseHandled).isFalse()
        assertThat(performedActions).containsExactly(MessageAction.DELETE to "message-1")
    }

    private fun event(
        action: Int,
        keyCode: Int,
        repeat: Int = 0,
        metaState: Int = 0,
        deviceId: Int = 1,
        flags: Int = 0,
    ) = KeyEvent(1L, 2L, action, keyCode, repeat, metaState, deviceId, 0, flags)

    private enum class MessageAction {
        DELETE,
        ARCHIVE,
        TOGGLE_FLAGGED,
    }
}
