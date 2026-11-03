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
class PhoneKeyShortcutHandlerTest {
    private var phoneIdle = true
    private var focusedMessage: String? = "message-1"
    private val shortcuts = mutableMapOf(
        KeyEvent.KEYCODE_CALL to MessageAction.DELETE,
        KeyEvent.KEYCODE_ENDCALL to MessageAction.ARCHIVE,
    )
    private val performedActions = mutableListOf<Pair<MessageAction, String>>()
    private val testSubject = PhoneKeyShortcutHandler(
        isPhoneIdle = { phoneIdle },
        getShortcut = shortcuts::get,
        getTarget = { focusedMessage },
        performAction = { action, target -> performedActions.add(action to target) },
    )

    @Test
    fun `each phone key should perform each configured action once when idle`() {
        // Arrange
        val expectedActions = mutableListOf<Pair<MessageAction, String>>()

        // Act
        for (keyCode in listOf(KeyEvent.KEYCODE_CALL, KeyEvent.KEYCODE_ENDCALL)) {
            for (action in MessageAction.entries) {
                shortcuts[keyCode] = action
                assertThat(testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_DOWN, keyCode))).isTrue()
                assertThat(testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_UP, keyCode))).isTrue()
                expectedActions.add(action to "message-1")
            }
        }

        // Assert
        assertThat(performedActions).containsExactly(*expectedActions.toTypedArray())
    }

    @Test
    fun `busy or unknown phone state should leave both keys completely unhandled`() {
        // Arrange
        phoneIdle = false

        // Act and assert
        for (keyCode in listOf(KeyEvent.KEYCODE_CALL, KeyEvent.KEYCODE_ENDCALL)) {
            assertThat(testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_DOWN, keyCode))).isFalse()
            assertThat(testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_UP, keyCode))).isFalse()
        }
        assertThat(performedActions).isEmpty()
    }

    @Test
    fun `a call arriving before release should pass the release through and discard the shortcut`() {
        // Arrange
        testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CALL))

        // Act
        phoneIdle = false
        val busyReleaseHandled = testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CALL))
        phoneIdle = true
        val staleReleaseHandled = testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CALL))

        // Assert
        assertThat(busyReleaseHandled).isFalse()
        assertThat(staleReleaseHandled).isFalse()
        assertThat(performedActions).isEmpty()
    }

    @Test
    fun `changing focused message before release should not apply the action to either message`() {
        // Arrange
        testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CALL))

        // Act
        focusedMessage = "message-2"
        val handled = testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CALL))

        // Assert
        assertThat(handled).isTrue()
        assertThat(performedActions).isEmpty()
    }

    @Test
    fun `changing or disabling an action before release should discard the pending action`() {
        // Arrange
        testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CALL))

        // Act
        shortcuts[KeyEvent.KEYCODE_CALL] = MessageAction.ARCHIVE
        testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CALL))
        testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENDCALL))
        shortcuts.remove(KeyEvent.KEYCODE_ENDCALL)
        testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENDCALL))

        // Assert
        assertThat(performedActions).isEmpty()
    }

    @Test
    fun `unconfigured shortcuts and absent focused messages should leave telephone keys unhandled`() {
        // Arrange
        shortcuts.remove(KeyEvent.KEYCODE_CALL)
        focusedMessage = null

        // Act and assert
        for (keyCode in listOf(KeyEvent.KEYCODE_CALL, KeyEvent.KEYCODE_ENDCALL)) {
            assertThat(testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_DOWN, keyCode))).isFalse()
            assertThat(testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_UP, keyCode))).isFalse()
        }
        assertThat(performedActions).isEmpty()
    }

    @Test
    fun `key repeats should not produce additional actions and a cancelled release should do nothing`() {
        // Arrange
        testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CALL))

        // Act
        val repeatHandled = testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CALL, repeat = 2))
        testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CALL))
        testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENDCALL))
        val cancelledReleaseHandled = testSubject.dispatchKeyEvent(
            event(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENDCALL, flags = KeyEvent.FLAG_CANCELED),
        )
        val orphanRepeatHandled = testSubject.dispatchKeyEvent(
            event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENDCALL, repeat = 1),
        )

        // Assert
        assertThat(repeatHandled).isTrue()
        assertThat(cancelledReleaseHandled).isTrue()
        assertThat(orphanRepeatHandled).isFalse()
        assertThat(performedActions).containsExactly(MessageAction.DELETE to "message-1")
    }

    @Test
    fun `a release from another input device must not trigger a pending action`() {
        // Arrange
        testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CALL, deviceId = 1))

        // Act
        val unrelatedReleaseHandled = testSubject.dispatchKeyEvent(
            event(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CALL, deviceId = 2),
        )
        val matchingReleaseHandled = testSubject.dispatchKeyEvent(
            event(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CALL, deviceId = 1),
        )

        // Assert
        assertThat(unrelatedReleaseHandled).isFalse()
        assertThat(matchingReleaseHandled).isTrue()
        assertThat(performedActions).containsExactly(MessageAction.DELETE to "message-1")
    }

    @Test
    fun `modified keys and non-telephone events should not be consumed`() {
        // Arrange
        val events = listOf(
            event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CALL, metaState = KeyEvent.META_CTRL_ON),
            event(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CALL, metaState = KeyEvent.META_CTRL_ON),
            event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HOME),
            event(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_POWER),
            event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_1),
            event(KeyEvent.ACTION_MULTIPLE, KeyEvent.KEYCODE_CALL),
        )

        // Act and assert
        events.forEach { assertThat(testSubject.dispatchKeyEvent(it)).isFalse() }
        assertThat(performedActions).isEmpty()
    }

    @Test
    fun `reset should discard an unfinished phone-key action`() {
        // Arrange
        testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CALL))

        // Act
        testSubject.reset()
        val handled = testSubject.dispatchKeyEvent(event(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CALL))

        // Assert
        assertThat(handled).isFalse()
        assertThat(performedActions).isEmpty()
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
