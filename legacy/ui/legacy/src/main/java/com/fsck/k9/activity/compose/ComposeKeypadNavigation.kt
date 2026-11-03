package com.fsck.k9.activity.compose

import android.app.Activity
import android.graphics.Rect
import android.text.Selection
import android.text.Spannable
import android.view.FocusFinder
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.fsck.k9.ui.R
import net.thunderbird.core.android.common.keypad.KeypadWindowInstaller

/** Explicit field navigation for twelve-key phones, without consuming characters in text editors. */
class ComposeKeypadNavigation(
    private val activity: Activity,
    private val isEnabled: () -> Boolean,
) {
    fun prepareMenu(menu: Menu) {
        val enabled = isEnabled()
        val itemIds = listOf(
            R.id.keypad_compose_next_field,
            R.id.keypad_compose_previous_field,
            R.id.keypad_compose_body,
            R.id.keypad_compose_attachments,
        )
        for (id in itemIds) {
            menu.findItem(id)?.isVisible = enabled
        }
        menu.findItem(R.id.keypad_compose_attachments)?.isEnabled =
            (activity.findViewById<ViewGroup>(R.id.attachments)?.childCount ?: 0) > 0
    }

    fun showAttachmentMenu(menu: Menu, installer: () -> KeypadWindowInstaller): Boolean {
        if (!isEnabled()) return false
        ComposeAttachmentMenu.show(activity, menu, installer(), isEnabled)
        return true
    }

    fun onMenuItemSelected(item: MenuItem): Boolean {
        if (!isEnabled()) return false
        return when (item.itemId) {
            R.id.keypad_compose_next_field -> {
                moveToField(View.FOCUS_FORWARD)
                true
            }

            R.id.keypad_compose_previous_field -> {
                moveToField(View.FOCUS_BACKWARD)
                true
            }

            R.id.keypad_compose_body -> {
                activity.findViewById<View>(R.id.message_content)?.let { focusAndReveal(it) }
                true
            }

            R.id.keypad_compose_attachments -> {
                val attachments = activity.findViewById<ViewGroup>(R.id.attachments)
                attachments?.getChildAt(0)?.findViewById<View>(R.id.attachment_delete)?.let { focusAndReveal(it) }
                true
            }

            else -> false
        }
    }

    private fun moveToField(direction: Int) {
        val root = activity.window.decorView as ViewGroup
        val originalFocus = activity.currentFocus
        val visited = mutableSetOf<View>()
        val focusFinder = FocusFinder.getInstance()
        val target = generateSequence(focusFinder.findNextFocus(root, originalFocus, direction)) { previous ->
            focusFinder.findNextFocus(root, previous, direction)
        }.takeWhile { it != originalFocus && visited.add(it) }
            .firstOrNull { it.onCheckIsTextEditor() }
        target?.let { focusAndReveal(it, direction) }
    }

    private fun focusAndReveal(target: View, direction: Int = View.FOCUS_FORWARD): Boolean {
        val textView = target as? TextView
        val selectionStart = textView?.selectionStart ?: -1
        val selectionEnd = textView?.selectionEnd ?: -1
        if (!target.requestFocus(direction)) return false
        val text = textView?.text as? Spannable
        if (text != null && selectionStart >= 0 && selectionEnd >= 0) {
            Selection.setSelection(
                text,
                selectionStart.coerceAtMost(text.length),
                selectionEnd.coerceAtMost(text.length),
            )
        }
        target.post {
            val rectangle = Rect()
            target.getFocusedRect(rectangle)
            target.requestRectangleOnScreen(rectangle)
        }
        return true
    }

    fun removeAttachment(container: ViewGroup, attachment: View, messageBody: View) {
        val restoreFocus = isEnabled() && attachment.hasFocus()
        val index = container.indexOfChild(attachment)
        container.removeView(attachment)
        if (!restoreFocus) return
        val next = container.getChildAt(index)?.findViewById<View>(R.id.attachment_delete)
        val previous = container.getChildAt(index - 1)?.findViewById<View>(R.id.attachment_delete)
        sequenceOf(next, previous, messageBody).filterNotNull().any { focusAndReveal(it) }
    }
}
