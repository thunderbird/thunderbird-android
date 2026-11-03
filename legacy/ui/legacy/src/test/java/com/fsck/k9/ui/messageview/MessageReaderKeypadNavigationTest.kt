package com.fsck.k9.ui.messageview

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isGreaterThan
import assertk.assertions.isLessThan
import assertk.assertions.isSameInstanceAs
import assertk.assertions.isTrue
import com.fsck.k9.view.NonLockingScrollView
import com.fsck.k9.view.MessageWebView
import net.thunderbird.core.android.testing.RobolectricTest
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowWebView
import org.robolectric.util.ReflectionHelpers.ClassParameter

@Config(sdk = [26, 31, 36], shadows = [MessageReaderKeypadNavigationTest.FocusableWebViewShadow::class])
class MessageReaderKeypadNavigationTest : RobolectricTest() {
    private lateinit var scrollView: NonLockingScrollView
    private lateinit var body: WideMessageWebView
    private lateinit var reply: Button
    private lateinit var attachments: Button
    private lateinit var testSubject: MessageReaderKeypadNavigation

    @Before
    fun setUp() {
        val activityController = Robolectric.buildActivity(Activity::class.java).setup()
        val activity = activityController.get()
        scrollView = NonLockingScrollView(activity).apply {
            setKeypadNavigationEnabled(true)
            isSmoothScrollingEnabled = false
        }
        reply = Button(activity).apply {
            text = "Reply"
            isFocusableInTouchMode = true
        }
        attachments = Button(activity).apply {
            text = "Attachment"
            isFocusableInTouchMode = true
        }
        body = WideMessageWebView(activity).apply {
            isFocusable = true
            isFocusableInTouchMode = true
        }
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(reply, LinearLayout.LayoutParams(400, 80))
            addView(body, LinearLayout.LayoutParams(400, 3000))
            addView(attachments, LinearLayout.LayoutParams(400, 80))
        }
        scrollView.addView(content, FrameLayout.LayoutParams(400, LinearLayout.LayoutParams.WRAP_CONTENT))
        activity.setContentView(scrollView)
        activityController.windowFocusChanged(true)
        scrollView.measure(
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY),
        )
        scrollView.layout(0, 0, 400, 600)
        assertThat(body.isAttachedToWindow).isTrue()
        assertThat(body.hasWindowFocus()).isTrue()
        assertThat(body.width).isEqualTo(400)
        assertThat(body.height).isEqualTo(3000)
        assertThat(body.requestFocus()).isTrue()
        assertThat(body.hasFocus()).isTrue()
        assertThat(scrollView.rootView.findFocus()).isSameInstanceAs(body)
        scrollView.scrollTo(0, 0)
        testSubject = MessageReaderKeypadNavigation(
            scrollView = scrollView,
            body = { body },
            reply = { reply },
            attachments = { attachments },
        )
    }

    @Test
    fun `direction down and held repeats scroll the outer document while body remains focused`() {
        // Act
        assertThat(dispatch(KeyEvent.KEYCODE_DPAD_DOWN)).isTrue()
        val firstOffset = scrollView.scrollY
        assertThat(dispatch(KeyEvent.KEYCODE_DPAD_DOWN, repeat = 1)).isTrue()
        assertThat(dispatch(KeyEvent.KEYCODE_DPAD_DOWN, repeat = 2)).isTrue()
        val repeatedOffset = scrollView.scrollY
        assertThat(dispatch(KeyEvent.KEYCODE_DPAD_DOWN, action = KeyEvent.ACTION_UP)).isTrue()

        // Assert
        assertThat(firstOffset).isGreaterThan(0)
        assertThat(repeatedOffset).isGreaterThan(firstOffset)
        assertThat(scrollView.scrollY).isEqualTo(repeatedOffset)
        assertThat(body.hasFocus()).isTrue()
    }

    @Test
    fun `paging keeps overlap with large fonts and top and bottom reach document boundaries`() {
        // Arrange
        @Suppress("DEPRECATION")
        scrollView.resources.updateConfiguration(
            Configuration(scrollView.resources.configuration).apply { fontScale = 2f },
            scrollView.resources.displayMetrics,
        )

        // Act and assert
        press(KeyEvent.KEYCODE_8)
        val pageOffset = scrollView.scrollY
        assertThat(pageOffset).isGreaterThan(0)
        assertThat(pageOffset).isLessThan(scrollView.height)
        press(KeyEvent.KEYCODE_2)
        assertThat(scrollView.scrollY).isEqualTo(0)
        press(KeyEvent.KEYCODE_3)
        assertThat(scrollView.canScrollVertically(1)).isFalse()
        press(KeyEvent.KEYCODE_1)
        assertThat(scrollView.scrollY).isEqualTo(0)
    }

    @Test
    fun `a fresh direction press at the bottom can leave the body for the attachment control`() {
        // Arrange
        press(KeyEvent.KEYCODE_3)

        // Act and assert
        assertThat(dispatch(KeyEvent.KEYCODE_DPAD_DOWN)).isTrue()
        assertThat(dispatch(KeyEvent.KEYCODE_DPAD_DOWN, action = KeyEvent.ACTION_UP)).isTrue()
        assertThat(attachments.hasFocus()).isTrue()
        assertThat(dispatch(KeyEvent.KEYCODE_DPAD_UP)).isFalse()
    }

    @Test
    fun `reply and attachment shortcuts only focus and center retains normal activation`() {
        // Arrange
        var replies = 0
        var openedAttachments = 0
        reply.setOnClickListener { replies++ }
        attachments.setOnClickListener { openedAttachments++ }

        // Act
        press(KeyEvent.KEYCODE_7)

        // Assert
        assertThat(reply.hasFocus()).isTrue()
        assertThat(replies).isEqualTo(0)
        assertThat(dispatch(KeyEvent.KEYCODE_DPAD_CENTER)).isFalse()
        reply.dispatchKeyEvent(event(KeyEvent.KEYCODE_DPAD_CENTER))
        reply.dispatchKeyEvent(event(KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.ACTION_UP))
        shadowOf(Looper.getMainLooper()).idle()
        assertThat(replies).isEqualTo(1)

        // Act and assert
        press(KeyEvent.KEYCODE_9)
        assertThat(attachments.hasFocus()).isTrue()
        assertThat(openedAttachments).isEqualTo(0)
        press(KeyEvent.KEYCODE_5)
        assertThat(body.hasFocus()).isTrue()
    }

    @Test
    fun `focusing the body preserves its reading position after using the reply button`() {
        // Arrange
        press(KeyEvent.KEYCODE_8)
        press(KeyEvent.KEYCODE_7)
        scrollView.scrollTo(0, 400)

        // Act
        press(KeyEvent.KEYCODE_5)

        // Assert
        assertThat(body.hasFocus()).isTrue()
        assertThat(scrollView.scrollY).isEqualTo(400)
    }

    @Test
    fun `native focus returning to a wrap content message WebView preserves the outer scroll position`() {
        // Arrange
        val content = scrollView.getChildAt(0) as LinearLayout
        val messageBody = MessageWebView(scrollView.context).apply { isFocusableInTouchMode = true }
        content.removeView(body)
        content.addView(messageBody, 1, LinearLayout.LayoutParams(400, 3000))
        scrollView.measure(
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY),
        )
        scrollView.layout(0, 0, 400, 600)
        assertThat(messageBody.requestFocus()).isTrue()
        assertThat(attachments.requestFocus()).isTrue()
        scrollView.scrollTo(0, Int.MAX_VALUE)
        val previous = scrollView.scrollY

        // Act
        assertThat(messageBody.requestFocus()).isTrue()

        // Assert
        assertThat(scrollView.scrollY).isEqualTo(previous)
    }

    @Test
    fun `body shortcut reveals the body when a long attachment area has scrolled it offscreen`() {
        // Arrange
        attachments.layoutParams.height = 1000
        attachments.requestLayout()
        scrollView.measure(
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY),
        )
        scrollView.layout(0, 0, 400, 600)
        press(KeyEvent.KEYCODE_3)
        val previous = scrollView.scrollY
        assertThat(body.getGlobalVisibleRect(Rect())).isFalse()

        // Act
        press(KeyEvent.KEYCODE_5)

        // Assert
        assertThat(body.hasFocus()).isTrue()
        assertThat(body.getGlobalVisibleRect(Rect())).isTrue()
        assertThat(scrollView.scrollY).isLessThan(previous)
    }

    @Test
    fun `wide HTML can be panned repeatedly without switching messages or scrolling vertically`() {
        // Act
        assertThat(dispatch(KeyEvent.KEYCODE_6)).isTrue()
        assertThat(dispatch(KeyEvent.KEYCODE_6, repeat = 1)).isTrue()
        assertThat(dispatch(KeyEvent.KEYCODE_6, action = KeyEvent.ACTION_UP)).isTrue()

        // Assert
        assertThat(body.horizontalOffset).isEqualTo(600)
        assertThat(scrollView.scrollY).isEqualTo(0)

        // Act and assert
        press(KeyEvent.KEYCODE_4)
        assertThat(body.horizontalOffset).isEqualTo(300)
    }

    @Test
    fun `editors modified keys and system keys are never consumed`() {
        // Arrange
        body.editing = true

        // Act and assert
        assertThat(dispatch(KeyEvent.KEYCODE_8)).isFalse()
        body.editing = false
        val systemKeys = listOf(
            KeyEvent.KEYCODE_CALL,
            KeyEvent.KEYCODE_ENDCALL,
            KeyEvent.KEYCODE_POWER,
            KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_TAB,
        )
        for (keyCode in systemKeys) {
            assertThat(dispatch(keyCode)).isFalse()
        }
        assertThat(testSubject.dispatchKeyEvent(event(KeyEvent.KEYCODE_8, modifiers = KeyEvent.META_CTRL_ON))).isFalse()
    }

    @Test
    fun `reset and orphan repeats cannot trigger a previous document press`() {
        // Arrange
        dispatch(KeyEvent.KEYCODE_8)
        val previous = scrollView.scrollY

        // Act
        testSubject.reset()

        // Assert
        assertThat(dispatch(KeyEvent.KEYCODE_8, repeat = 1)).isFalse()
        assertThat(dispatch(KeyEvent.KEYCODE_8, action = KeyEvent.ACTION_UP)).isFalse()
        assertThat(scrollView.scrollY).isEqualTo(previous)
    }

    @Test
    fun `initial load moves focus from the reader container to the body once without changing saved scroll`() {
        // Arrange
        focusReaderContainer()
        scrollView.scrollTo(0, 420)
        val initialFocus = createInitialFocus()
        initialFocus.onBodyRendered()

        // Act
        assertThat(initialFocus.tryFocusBody()).isTrue()

        // Assert
        assertThat(body.hasFocus()).isTrue()
        assertThat(scrollView.scrollY).isEqualTo(420)
        reply.requestFocus()
        assertThat(initialFocus.tryFocusBody()).isFalse()
        assertThat(reply.hasFocus()).isTrue()
    }

    @Test
    fun `late content completion never takes focus from an explicitly chosen header or editor`() {
        // Arrange
        val initialFocus = createInitialFocus()
        initialFocus.onBodyRendered()
        initialFocus.onUserInteraction()
        reply.requestFocus()

        // Act and assert
        assertThat(initialFocus.tryFocusBody()).isFalse()
        assertThat(reply.hasFocus()).isTrue()
        val editorFocus = createInitialFocus()
        editorFocus.onBodyRendered()
        body.editing = true
        body.requestFocus()
        assertThat(editorFocus.tryFocusBody()).isFalse()
    }

    @Test
    fun `automatic header focus during startup does not cancel initial body focus`() {
        // Arrange
        reply.requestFocus()
        val initialFocus = createInitialFocus()
        initialFocus.onBodyRendered()

        // Act
        assertThat(initialFocus.tryFocusBody()).isTrue()

        // Assert
        assertThat(body.hasFocus()).isTrue()
    }

    @Test
    fun `automatic toolbar navigation focus yields to the loaded body without changing saved scroll`() {
        // Arrange
        val toolbar = addToolbar()
        val navigateUp = toolbar.getChildAt(0)
        assertThat(navigateUp.requestFocus()).isTrue()
        scrollView.scrollTo(0, 420)
        val initialFocus = createInitialFocus(defaultFocusContainer = { toolbar })
        initialFocus.onBodyRendered()

        // Act
        assertThat(initialFocus.tryFocusBody()).isTrue()

        // Assert
        assertThat(body.hasFocus()).isTrue()
        assertThat(scrollView.scrollY).isEqualTo(420)
    }

    @Test
    fun `user interaction with the toolbar before rendering prevents late body focus`() {
        // Arrange
        val toolbar = addToolbar()
        val navigateUp = toolbar.getChildAt(0)
        val initialFocus = createInitialFocus(defaultFocusContainer = { toolbar })
        assertThat(navigateUp.requestFocus()).isTrue()
        initialFocus.onUserInteraction()
        initialFocus.onBodyRendered()

        // Act and assert
        assertThat(initialFocus.tryFocusBody()).isFalse()
        assertThat(navigateUp.hasFocus()).isTrue()
    }

    @Test
    fun `focus outside the reader and default toolbar is retained after rendering`() {
        // Arrange
        val toolbar = addToolbar()
        val otherContainer = addToolbar()
        val outsideControl = otherContainer.getChildAt(0)
        assertThat(outsideControl.requestFocus()).isTrue()
        val initialFocus = createInitialFocus(defaultFocusContainer = { toolbar })
        initialFocus.onBodyRendered()

        // Act and assert
        assertThat(initialFocus.tryFocusBody()).isFalse()
        assertThat(outsideControl.hasFocus()).isTrue()
    }

    @Test
    fun `inactive reader waits for activation but any navigation before completion cancels automatic focus`() {
        // Arrange
        focusReaderContainer()
        var ready = false
        val initialFocus = createInitialFocus { ready }
        initialFocus.onBodyRendered()
        assertThat(initialFocus.tryFocusBody()).isFalse()

        // Act
        initialFocus.onUserInteraction()
        ready = true

        // Assert
        assertThat(initialFocus.tryFocusBody()).isFalse()
        assertThat(scrollView.isFocused).isTrue()
    }

    @Test
    fun `preloaded inactive page can acquire initial body focus when it first becomes active`() {
        // Arrange
        focusReaderContainer()
        var ready = false
        val initialFocus = createInitialFocus { ready }
        initialFocus.onBodyRendered()
        assertThat(initialFocus.tryFocusBody()).isFalse()

        // Act
        ready = true

        // Assert
        assertThat(initialFocus.tryFocusBody()).isTrue()
        assertThat(body.hasFocus()).isTrue()
    }

    private fun createInitialFocus(
        defaultFocusContainer: () -> View? = { null },
        isReady: () -> Boolean = { true },
    ) = MessageReaderInitialFocus(
        scrollView = scrollView,
        body = { body },
        isReaderReady = isReady,
        defaultFocusContainer = defaultFocusContainer,
    )

    private fun addToolbar(): ViewGroup {
        val toolbar = LinearLayout(scrollView.context).apply {
            addView(Button(context).apply { isFocusableInTouchMode = true }, LinearLayout.LayoutParams(80, 80))
        }
        (scrollView.parent as ViewGroup).addView(toolbar, FrameLayout.LayoutParams(400, 80))
        toolbar.measure(
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(80, View.MeasureSpec.EXACTLY),
        )
        toolbar.layout(0, 0, 400, 80)
        return toolbar
    }

    private fun focusReaderContainer() {
        val previous = scrollView.descendantFocusability
        scrollView.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        scrollView.isFocusableInTouchMode = true
        assertThat(scrollView.requestFocus()).isTrue()
        scrollView.descendantFocusability = previous
        assertThat(scrollView.isFocused).isTrue()
    }

    private fun press(keyCode: Int) {
        dispatch(keyCode)
        dispatch(keyCode, action = KeyEvent.ACTION_UP)
    }

    private fun dispatch(keyCode: Int, action: Int = KeyEvent.ACTION_DOWN, repeat: Int = 0): Boolean =
        testSubject.dispatchKeyEvent(event(keyCode, action, repeat))

    private fun event(keyCode: Int, action: Int = KeyEvent.ACTION_DOWN, repeat: Int = 0, modifiers: Int = 0) =
        KeyEvent(0, 0, action, keyCode, repeat, modifiers, 1, 0)

    /**
     * Robolectric's empty WebView provider does not delegate focus or frame changes to the framework.
     * Use the real View/ViewGroup methods that WebView's PrivateAccess exposes to its provider. This
     * retains measured geometry, focus ownership, ancestor restrictions, and parent focus callbacks.
     */
    @Implements(WebView::class)
    class FocusableWebViewShadow : ShadowWebView() {
        @RealObject
        private lateinit var webView: WebView

        @Implementation
        fun requestFocus(direction: Int, previouslyFocusedRect: Rect?): Boolean = Shadow.directlyOn(
            webView,
            ViewGroup::class.java,
            "requestFocus",
            ClassParameter.from(Int::class.javaPrimitiveType, direction),
            ClassParameter.from(Rect::class.java, previouslyFocusedRect),
        )

        @Implementation
        fun findFocus(): View? = Shadow.directlyOn(webView, ViewGroup::class.java, "findFocus")

        @Implementation
        fun setFrame(left: Int, top: Int, right: Int, bottom: Int): Boolean = Shadow.directlyOn(
            webView,
            View::class.java,
            "setFrame",
            ClassParameter.from(Int::class.javaPrimitiveType, left),
            ClassParameter.from(Int::class.javaPrimitiveType, top),
            ClassParameter.from(Int::class.javaPrimitiveType, right),
            ClassParameter.from(Int::class.javaPrimitiveType, bottom),
        )
    }

    private class WideMessageWebView(context: Context) : WebView(context) {
        var horizontalOffset = 0
        var editing = false

        override fun onCheckIsTextEditor(): Boolean = editing

        override fun canScrollHorizontally(direction: Int): Boolean =
            if (direction < 0) horizontalOffset > 0 else horizontalOffset < 1200

        override fun scrollBy(x: Int, y: Int) {
            horizontalOffset = (horizontalOffset + x).coerceIn(0, 1200)
        }
    }
}
