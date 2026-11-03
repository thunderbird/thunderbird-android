package com.fsck.k9.ui.messageview

import android.graphics.Rect
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.ScrollView
import kotlin.math.roundToInt

private const val READING_LINE_STEP_DP = 48
private const val PAGE_OVERLAP_DIVISOR = 4
private const val HORIZONTAL_PAGE_FRACTION = 0.75f

/** Scrolls the reader's outer document, which contains its wrap-content WebView and attachments. */
internal class MessageReaderKeypadNavigation(
    private val scrollView: ScrollView,
    private val body: () -> View?,
    private val reply: () -> View?,
    private val attachments: () -> View?,
) {
    private val pressedKeys = mutableSetOf<Key>()

    @Suppress("ReturnCount", "CyclomaticComplexMethod")
    fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val key = Key(event.deviceId, event.keyCode)
        if (event.action == KeyEvent.ACTION_UP) return pressedKeys.remove(key)
        if (event.action != KeyEvent.ACTION_DOWN || !event.hasNoModifiers() || event.isCanceled) return false
        if (scrollView.rootView.findFocus()?.onCheckIsTextEditor() == true) return false
        if (event.repeatCount != 0 && key !in pressedKeys) return false

        val handled = when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> moveBody(key, View.FOCUS_UP, -lineStep)
            KeyEvent.KEYCODE_DPAD_DOWN -> moveBody(key, View.FOCUS_DOWN, lineStep)
            KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_MOVE_HOME -> consume { scrollView.scrollTo(scrollView.scrollX, 0) }
            KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_MOVE_END -> consume {
                scrollView.scrollTo(scrollView.scrollX, maximumScroll)
            }
            KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_PAGE_UP -> consume { scrollVertically(-pageStep) }
            KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_PAGE_DOWN -> consume { scrollVertically(pageStep) }
            KeyEvent.KEYCODE_4 -> scrollHorizontally(-1)
            KeyEvent.KEYCODE_6 -> scrollHorizontally(1)
            KeyEvent.KEYCODE_5 -> focusBody()
            KeyEvent.KEYCODE_7 -> focusControl(reply())
            KeyEvent.KEYCODE_9 -> focusControl(attachments())
            else -> false
        }
        if (handled) pressedKeys.add(key)
        // Keep a consumed press paired at the document boundary. A fresh press may move focus out of the body.
        return handled || key in pressedKeys
    }

    fun reset() {
        pressedKeys.clear()
    }

    private val viewportHeight: Int
        get() = (scrollView.height - scrollView.paddingTop - scrollView.paddingBottom).coerceAtLeast(1)

    private val lineStep: Int
        get() = (READING_LINE_STEP_DP * scrollView.resources.displayMetrics.density *
            scrollView.resources.configuration.fontScale)
            .roundToInt().coerceIn(1, (viewportHeight / 2).coerceAtLeast(1))

    private val pageStep: Int
        get() = (viewportHeight - lineStep.coerceAtMost(viewportHeight / PAGE_OVERLAP_DIVISOR)).coerceAtLeast(1)

    private val maximumScroll: Int
        get() {
            val child = scrollView.getChildAt(0) ?: return 0
            val bottomMargin = (child.layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin ?: 0
            return (child.bottom + bottomMargin + scrollView.paddingBottom - scrollView.height).coerceAtLeast(0)
        }

    private fun scrollVertically(distance: Int): Boolean {
        val previous = scrollView.scrollY
        scrollView.scrollTo(scrollView.scrollX, (previous + distance).coerceIn(0, maximumScroll))
        return scrollView.scrollY != previous
    }

    private fun moveBody(key: Key, direction: Int, distance: Int): Boolean {
        val focusedBody = body()?.takeIf { it.hasFocus() } ?: return false
        return when {
            scrollVertically(distance) -> true
            key in pressedKeys -> true
            else -> {
                // WebView may consume arrows even though the outer document has reached its boundary.
                val next = focusedBody.focusSearch(direction)?.takeUnless { it === focusedBody }
                focusControl(next)
            }
        }
    }

    private fun scrollHorizontally(direction: Int): Boolean {
        val webView = body() as? WebView ?: return false
        return if (webView.hasFocus() && webView.canScrollHorizontally(direction)) {
            webView.scrollBy(direction * (scrollView.width * HORIZONTAL_PAGE_FRACTION).roundToInt().coerceAtLeast(1), 0)
            true
        } else {
            false
        }
    }

    private fun focusBody(): Boolean {
        val bodyView = body() ?: return false
        val wasVisible = bodyView.getGlobalVisibleRect(Rect())
        val previousX = scrollView.scrollX
        val previousY = scrollView.scrollY
        val focused = focusControl(bodyView)
        if (wasVisible) {
            scrollView.scrollTo(previousX, previousY)
        } else if (focused) {
            bodyView.requestRectangleOnScreen(
                Rect(0, 0, bodyView.width, bodyView.height.coerceAtMost(viewportHeight)),
                true,
            )
        }
        return focused
    }

    private fun focusControl(view: View?): Boolean =
        view?.takeIf { it.isShown && it.isEnabled }?.requestFocus() == true

    private inline fun consume(action: () -> Unit): Boolean {
        action()
        return true
    }

    private data class Key(val deviceId: Int, val keyCode: Int)
}
