package com.fsck.k9.ui.messageview

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ScrollView
import com.fsck.k9.ui.R
import kotlin.math.absoluteValue

/**
 * A view that listens to touch events to make sure [R.id.message_viewpager] doesn't get to see events that should be
 * handled by [R.id.message_scrollview] or [R.id.message_content].
 *
 * We allow the view pager to listen to touch events until we know it's a gesture that will be handled by the scroll
 * view or the web view.
 * If we instead hid events from the view pager until we knew the scroll view or the web view don't want to handle the
 * gesture, we'd risk minimal fling gestures ([MotionEvent.ACTION_DOWN], [MotionEvent.ACTION_MOVE],
 * [MotionEvent.ACTION_UP]) not working because the single [MotionEvent.ACTION_MOVE] event would be invisible to the
 * view pager.
 */
class TouchInterceptView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private var initialX: Float = 0f
    private var initialY: Float = 0f
    private var initialRawX = 0
    private var initialRawY = 0

    private val webViewScreenLocation = IntArray(2)
    private val webViewRect = Rect()

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        handleOnInterceptTouchEvent(event)
        return super.onInterceptTouchEvent(event)
    }

    private fun handleOnInterceptTouchEvent(event: MotionEvent) {
        val webView = findViewById<WebView>(R.id.message_content) ?: return
        val scrollView = findViewById<ScrollView>(R.id.message_scrollview) ?: return
        val scrollViewParent = scrollView.parent ?: return

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialX = event.x
                initialY = event.y
                initialRawX = event.rawX.toInt()
                initialRawY = event.rawY.toInt()
                scrollViewParent.requestDisallowInterceptTouchEvent(false)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                // If a second finger/pointer is involved, never allow parents of the ScrollView to intercept
                scrollViewParent.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = initialX - event.x
                val deltaY = initialY - event.y

                val absoluteDeltaX = deltaX.absoluteValue
                val absoluteDeltaY = deltaY.absoluteValue

                if (absoluteDeltaY > touchSlop && absoluteDeltaY > absoluteDeltaX &&
                    (scrollView.canScrollVertically(deltaY.toInt()) || webView.canScrollVertically(deltaY.toInt()))
                ) {
                    scrollViewParent.requestDisallowInterceptTouchEvent(true)
                } else if (absoluteDeltaX > touchSlop && absoluteDeltaX > absoluteDeltaY &&
                    webView.canScrollHorizontally(deltaX.toInt())
                ) {
                    webView.getHitRect(webViewRect)
                    webView.getLocationOnScreen(webViewScreenLocation)
                    webViewRect.offset(webViewScreenLocation[0], webViewScreenLocation[1])

                    if (webViewRect.contains(initialRawX, initialRawY)) {
                        scrollViewParent.requestDisallowInterceptTouchEvent(true)
                    }
                }
            }
        }
    }
}
