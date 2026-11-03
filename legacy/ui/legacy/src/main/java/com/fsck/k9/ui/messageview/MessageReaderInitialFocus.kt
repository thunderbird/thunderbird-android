package com.fsck.k9.ui.messageview

import android.view.View
import android.widget.ScrollView

/** Makes a loaded reader usable immediately without taking focus back after the user has navigated. */
internal class MessageReaderInitialFocus(
    private val scrollView: ScrollView,
    private val body: () -> View?,
    private val isReaderReady: () -> Boolean,
    private val defaultFocusContainer: () -> View? = { null },
) {
    private var bodyRendered = false
    private var pending = true

    fun onBodyRendered() {
        bodyRendered = true
    }

    fun onUserInteraction() {
        pending = false
    }

    fun tryFocusBody(): Boolean {
        val bodyView = body()
        val focused = scrollView.rootView.findFocus()
        return when {
            !pending || !bodyRendered || !isReaderReady() -> false
            bodyView == null || !bodyView.isShown -> false
            focused?.onCheckIsTextEditor() == true -> {
                pending = false
                false
            }
            focused?.isAccessibilityFocused == true -> {
                pending = false
                false
            }
            focused != null && !isDefaultReaderFocus(focused) -> {
                pending = false
                false
            }
            else -> {
                val previousX = scrollView.scrollX
                val previousY = scrollView.scrollY
                val acquiredFocus = bodyView.requestFocus()
                scrollView.scrollTo(previousX, previousY)
                if (acquiredFocus) pending = false
                acquiredFocus
            }
        }
    }

    private fun isDefaultReaderFocus(view: View): Boolean {
        val withinReader = isAncestor(view, scrollView) || isAncestor(scrollView, view)
        val withinDefaultContainer = defaultFocusContainer()?.let { isAncestor(it, view) } == true
        return withinReader || withinDefaultContainer
    }

    private fun isAncestor(ancestor: View, view: View): Boolean {
        var candidate: View? = view
        while (candidate != null && candidate !== ancestor) candidate = candidate.parent as? View
        return candidate === ancestor
    }
}
