package net.thunderbird.app.common.activity

import android.app.Activity
import android.view.Menu
import androidx.appcompat.widget.PopupMenu
import net.thunderbird.app.common.R

/** Gives forms without an options toolbar an explicit way to leave text fields on twelve-key phones. */
internal class KeypadFallbackMenu(private val activity: Activity) {
    fun navigation(next: () -> Unit, previous: () -> Unit, back: () -> Unit): Menu = createMenu().apply {
        add(Menu.NONE, NEXT, Menu.NONE, R.string.keypad_next_control).setOnMenuItemClickListener {
            next()
            true
        }
        add(Menu.NONE, PREVIOUS, Menu.NONE, R.string.keypad_previous_control)
            .setOnMenuItemClickListener {
                previous()
                true
            }
        add(Menu.NONE, BACK, Menu.NONE, R.string.keypad_back).setOnMenuItemClickListener {
            back()
            true
        }
    }

    fun activation(enable: () -> Unit): Menu = createMenu().apply {
        add(Menu.NONE, ENABLE, Menu.NONE, R.string.keypad_enable).setOnMenuItemClickListener {
            enable()
            true
        }
        add(Menu.NONE, CANCEL, Menu.NONE, android.R.string.cancel).setOnMenuItemClickListener { true }
    }

    private fun createMenu(): Menu = PopupMenu(activity, activity.window.decorView).menu

    private companion object {
        const val NEXT = 1
        const val PREVIOUS = 2
        const val BACK = 3
        const val ENABLE = 4
        const val CANCEL = 5
    }
}
