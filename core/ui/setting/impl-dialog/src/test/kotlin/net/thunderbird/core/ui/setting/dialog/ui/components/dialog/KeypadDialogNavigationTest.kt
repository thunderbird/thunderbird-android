package net.thunderbird.core.ui.setting.dialog.ui.components.dialog

import android.view.KeyEvent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.testing.onNodeWithText
import app.k9mail.core.ui.compose.testing.setContentWithTheme
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.dialog.ui.components.dialog.value.TextDialogView
import org.junit.Test
import net.thunderbird.core.ui.setting.dialog.R as DialogR

class KeypadDialogNavigationTest : ComposeTest() {
    @Test
    fun `F2 should dismiss a focused settings dialog only when keypad mode is enabled`() = runComposeTest {
        // Arrange
        var enabled = false
        var dismissCount = 0
        setContentWithTheme {
            CompositionLocalProvider(LocalKeypadNavigationEnabled provides { enabled }) {
                SettingDialogLayout(
                    title = "Setting",
                    icon = null,
                    onConfirmClick = {},
                    onDismissClick = {},
                    onDismissRequest = { dismissCount++ },
                ) {
                    TextBodyMedium("Dialog content")
                }
            }
        }
        val testSubject = onNodeWithText(DialogR.string.core_ui_setting_dialog_button_accept)
        testSubject.performSemanticsAction(SemanticsActions.RequestFocus) { it() }

        // Act
        testSubject.performKeyInput {
            keyDown(Key.F2)
            keyUp(Key.F2)
        }

        // Assert
        assertThat(dismissCount).isEqualTo(0)

        // Act
        enabled = true
        testSubject.performKeyInput {
            keyDown(Key.F2)
            keyUp(Key.F2)
        }

        // Assert
        assertThat(dismissCount).isEqualTo(1)
    }

    @Test
    fun `star and pound should move focus between real dialog buttons`() = runComposeTest {
        // Arrange
        setContentWithTheme {
            CompositionLocalProvider(LocalKeypadNavigationEnabled provides { true }) {
                SettingDialogLayout(
                    title = "Setting",
                    icon = null,
                    onConfirmClick = {},
                    onDismissClick = {},
                    onDismissRequest = {},
                ) {
                    TextBodyMedium("Dialog content")
                }
            }
        }
        val testSubject = onNodeWithText(DialogR.string.core_ui_setting_dialog_button_cancel)
        val accept = onNodeWithText(DialogR.string.core_ui_setting_dialog_button_accept)
        testSubject.performSemanticsAction(SemanticsActions.RequestFocus) { it() }

        // Act
        testSubject.performKeyInput {
            keyDown(Key.Pound)
            keyUp(Key.Pound)
        }

        // Assert
        accept.assertIsFocused()

        // Act
        accept.performKeyInput {
            keyDown(Key(KeyEvent.KEYCODE_STAR))
            keyUp(Key(KeyEvent.KEYCODE_STAR))
        }

        // Assert
        testSubject.assertIsFocused()
    }

    @Test
    fun `focused Compose text input should retain star and pound hardware characters`() = runComposeTest {
        // Arrange
        setContentWithTheme {
            CompositionLocalProvider(LocalKeypadNavigationEnabled provides { true }) {
                TextDialogView(
                    setting = SettingValue.Text(id = "text", title = { "Title" }, value = ""),
                    onConfirmClick = {},
                    onDismissClick = {},
                    onDismissRequest = {},
                )
            }
        }
        mainClock.advanceTimeBy(300L)
        val testSubject = onNode(hasSetTextAction())
        testSubject.performSemanticsAction(SemanticsActions.RequestFocus) { it() }

        // Act
        testSubject.performKeyInput {
            keyDown(Key(KeyEvent.KEYCODE_STAR))
            keyUp(Key(KeyEvent.KEYCODE_STAR))
            keyDown(Key.Pound)
            keyUp(Key.Pound)
        }

        // Assert
        testSubject.assertIsFocused()
        onNodeWithText("*#").assertExists()
    }

    @Test
    fun `F1 should move from a real Compose text field to a dialog button without applying or dismissing`() =
        runComposeTest {
            // Arrange
            var actionCount = 0
            setContentWithTheme {
                CompositionLocalProvider(LocalKeypadNavigationEnabled provides { true }) {
                    TextDialogView(
                        setting = SettingValue.Text(id = "server", title = { "Server" }, value = "mail.example"),
                        onConfirmClick = { actionCount++ },
                        onDismissClick = { actionCount++ },
                        onDismissRequest = { actionCount++ },
                    )
                }
            }
            mainClock.advanceTimeBy(300L)
            val testSubject = onNode(hasSetTextAction())
            testSubject.performSemanticsAction(SemanticsActions.RequestFocus) { it() }

            // Act
            testSubject.performKeyInput {
                keyDown(Key.F1)
                keyUp(Key.F1)
            }

            // Assert
            testSubject.assertIsNotFocused()
            val accept = hasText(getString(DialogR.string.core_ui_setting_dialog_button_accept), ignoreCase = true)
            val cancel = hasText(getString(DialogR.string.core_ui_setting_dialog_button_cancel), ignoreCase = true)
            onNode((accept or cancel) and isFocused()).assertExists()
            assertThat(actionCount).isEqualTo(0)
            onNodeWithText("mail.example").assertExists()
        }

    @Test
    fun `canceled or unmatched releases should not run dialog actions`() {
        // Arrange
        val testSubject = KeypadDialogKeyHandler()
        val actions = mutableListOf<KeypadDialogAction>()
        testSubject.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F2), true, false, actions::add)

        // Act
        testSubject.dispatchKeyEvent(
            KeyEvent(0L, 0L, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_F2, 0, 0, -1, 0, KeyEvent.FLAG_CANCELED),
            true,
            false,
            actions::add,
        )
        val unmatchedHandled = testSubject.dispatchKeyEvent(
            KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_F2),
            true,
            false,
            actions::add,
        )

        // Assert
        assertThat(actions).isEmpty()
        assertThat(unmatchedHandled).isFalse()
    }
}
