package com.fsck.k9.ui.messageview

import android.view.KeyEvent
import android.view.Window
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.fsck.k9.ui.helper.SizeFormatter
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.components.ui.bolt.atom.button.ButtonText
import net.thunderbird.components.ui.bolt.theme.thunderbird.ThunderbirdBoltTheme
import net.thunderbird.core.android.common.keypad.KeypadWindowInstaller
import net.thunderbird.core.android.testing.RobolectricTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowViewRootImpl
import org.robolectric.util.ReflectionHelpers

@Config(sdk = [26, 31, 36])
class AttachmentListModalBottomSheetKeypadTest : RobolectricTest() {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val installer = RecordingWindowInstaller()

    @Before
    fun setUp() {
        startKoin {
            modules(module { single<KeypadWindowInstaller> { installer } })
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `F2 should reach the attachment sheet window and dismiss without activating the underlying reader control`() {
        // Arrange
        var showAttachments by mutableStateOf(false)
        var underlyingClickCount = 0
        var dismissCount = 0
        val sizeFormatter = SizeFormatter(RuntimeEnvironment.getApplication().resources)
        composeTestRule.setContent {
            ThunderbirdBoltTheme {
                Column {
                    ButtonText(
                        text = "View attachments",
                        onClick = {
                            underlyingClickCount++
                            showAttachments = true
                        },
                    )
                    if (showAttachments) {
                        AttachmentListModalBottomSheet(
                            attachments = persistentListOf(),
                            sizeFormatter = sizeFormatter,
                            onDismissRequest = {
                                dismissCount++
                                showAttachments = false
                            },
                            onAttachmentClick = {},
                            onSaveClick = {},
                            onSaveAllClick = {},
                        )
                    }
                }
            }
        }
        val testSubject = composeTestRule.onNodeWithText("View attachments")
        testSubject.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        testSubject.performClick()
        composeTestRule.waitForIdle()

        // Assert - this must be the real bottom sheet's independently installed window.
        assertThat(installer.window).isNotNull()
        assertThat(installer.installCount).isEqualTo(1)

        // Act
        composeTestRule.runOnIdle {
            val window = requireNotNull(installer.window)
            val viewRoot = ReflectionHelpers.callInstanceMethod<Any>(window.decorView, "getViewRootImpl")
            Shadow.extract<ShadowViewRootImpl>(viewRoot).callWindowFocusChanged(true)
        }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle {
            val callback = requireNotNull(installer.window?.callback)
            callback.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F2))
            callback.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_F2))
        }
        composeTestRule.waitForIdle()

        // Assert
        assertThat(dismissCount).isEqualTo(1)
        assertThat(underlyingClickCount).isEqualTo(1)
        assertThat(installer.f2Received).isTrue()
        assertThat(installer.disposalCount).isEqualTo(1)
        testSubject.assertIsFocused()
    }

    /** An enabled installer fake; the production translator is covered in app-common. */
    private class RecordingWindowInstaller : KeypadWindowInstaller {
        var window: Window? = null
        var installCount = 0
        var disposalCount = 0
        var f2Received = false

        override fun install(window: Window): AutoCloseable {
            this.window = window
            installCount++
            val delegate = requireNotNull(window.callback)
            val callback = object : Window.Callback by delegate {
                override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                    if (event.keyCode != KeyEvent.KEYCODE_F2) return delegate.dispatchKeyEvent(event)
                    f2Received = true
                    if (event.action == KeyEvent.ACTION_UP) {
                        delegate.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK))
                        delegate.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK))
                    }
                    return true
                }
            }
            window.callback = callback
            return AutoCloseable {
                disposalCount++
                if (window.callback === callback) window.callback = delegate
            }
        }
    }
}
