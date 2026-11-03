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
class KeypadShortcutHandlerTest {
    private val actions = mutableListOf<String>()
    private val testSubject = KeypadShortcutHandler(mapOf(KeyEvent.KEYCODE_5 to "refresh"))

    @Test
    fun `inactive contexts should retain numeric input and native system keys`() {
        for (code in listOf(
            KeyEvent.KEYCODE_5,
            KeyEvent.KEYCODE_CALL,
            KeyEvent.KEYCODE_ENDCALL,
            KeyEvent.KEYCODE_HOME,
        )) {
            assertThat(dispatch(KeyEvent(KeyEvent.ACTION_DOWN, code), enabled = false)).isFalse()
            assertThat(dispatch(KeyEvent(KeyEvent.ACTION_UP, code), enabled = false)).isFalse()
        }
        assertThat(actions).isEmpty()
    }

    @Test
    fun `held refresh should execute once only after release`() {
        assertThat(dispatch(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_5))).isTrue()
        assertThat(dispatch(KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_5, 4))).isTrue()
        assertThat(actions).isEmpty()
        assertThat(dispatch(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_5))).isTrue()
        assertThat(actions).containsExactly("refresh")
    }

    @Test
    fun `entering an editor before release should cancel the shortcut and consume its paired release`() {
        dispatch(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_5))
        assertThat(dispatch(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_5), enabled = false)).isTrue()
        assertThat(actions).isEmpty()
    }

    @Test
    fun `modified or cancelled releases should not refresh`() {
        val modified = KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_5, 0, KeyEvent.META_CTRL_ON)
        assertThat(dispatch(modified)).isFalse()
        dispatch(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_5))
        val cancelled = KeyEvent.changeFlags(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_5), KeyEvent.FLAG_CANCELED)
        assertThat(dispatch(cancelled)).isTrue()
        assertThat(actions).isEmpty()
    }

    @Test
    fun `window loss should discard a pending action`() {
        dispatch(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_5))
        testSubject.reset()
        assertThat(dispatch(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_5))).isFalse()
        assertThat(actions).isEmpty()
    }

    private fun dispatch(event: KeyEvent, enabled: Boolean = true): Boolean =
        testSubject.dispatchKeyEvent(event, enabled, actions::add)
}
