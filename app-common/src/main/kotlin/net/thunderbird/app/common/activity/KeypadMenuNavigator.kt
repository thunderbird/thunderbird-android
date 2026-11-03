package net.thunderbird.app.common.activity

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.BaseAdapter
import android.widget.CheckedTextView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import net.thunderbird.core.android.common.keypad.KeypadWindowInstaller

/** Presents the existing menu items in a window that supports the optional keypad mappings. */
internal class KeypadMenuNavigator(
    private val activity: Activity,
    private val windowInstaller: KeypadWindowInstaller,
) {
    private val levels = mutableListOf<MenuLevel>()
    private var currentDialog: AlertDialog? = null
    private var disposeCurrentWindow: (() -> Unit)? = null

    fun showMenu(menu: Menu, preferredItemIds: List<Int> = emptyList()): Boolean {
        if (activity.isFinishing || activity.isDestroyed || visibleItems(menu).isEmpty()) return false

        close()
        levels.add(MenuLevel(menu, preferredItemIds))
        showCurrentLevel()
        return true
    }

    private fun showCurrentLevel() {
        val level = levels.lastOrNull() ?: return
        val items = prioritize(visibleItems(level.menu), level.preferredItemIds)
        val builder = AlertDialog.Builder(activity)
        val adapter = MenuItemAdapter(builder.context, items)
        val dialog = builder
            .setTitle((level.menu as? SubMenu)?.item?.title ?: activity.title)
            .setAdapter(adapter, null)
            .create()

        var installation: AutoCloseable? = null
        var disposeInitialSelection: (() -> Unit)? = null
        val disposeWindow = {
            disposeInitialSelection?.invoke()
            disposeInitialSelection = null
            installation?.close()
            installation = null
        }
        currentDialog = dialog
        disposeCurrentWindow = disposeWindow
        dialog.setOnCancelListener {
            if (currentDialog === dialog) returnToParent()
        }
        dialog.setOnDismissListener {
            disposeWindow()
            if (currentDialog === dialog) {
                currentDialog = null
                disposeCurrentWindow = null
                levels.clear()
            }
        }
        dialog.show()
        installation = dialog.window?.let(windowInstaller::install)
        dialog.listView.apply {
            setOnItemClickListener { _, _, position, _ -> selectItem(level, items[position]) }
            val initialPosition = items.indexOfFirst { it.itemId == level.selectedItemId && it.isEnabled }
                .takeIf { it >= 0 } ?: items.indexOfFirst { it.isEnabled }
            disposeInitialSelection = prepareInitialSelection(dialog, initialPosition)
        }
    }

    private fun prepareInitialSelection(dialog: AlertDialog, position: Int): () -> Unit {
        if (position < 0) return {}
        val list = dialog.listView
        var initialized = false
        val initialize = Runnable {
            val windowReady = dialog.isShowing && list.isLaidOut && list.hasWindowFocus()
            if (!initialized && windowReady) {
                initialized = true
                list.requestFocusFromTouch()
                list.setSelection(position)
            }
        }
        val layoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (!initialized) list.post(initialize)
        }
        val focusListener = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (hasFocus && !initialized) list.post(initialize)
        }
        list.addOnLayoutChangeListener(layoutListener)
        list.viewTreeObserver.addOnWindowFocusChangeListener(focusListener)
        list.post(initialize)
        return {
            initialized = true
            list.removeCallbacks(initialize)
            list.removeOnLayoutChangeListener(layoutListener)
            if (list.viewTreeObserver.isAlive) {
                list.viewTreeObserver.removeOnWindowFocusChangeListener(focusListener)
            }
        }
    }

    private fun selectItem(level: MenuLevel, item: MenuItem) {
        if (!item.isVisible || !item.isEnabled) return
        val subMenu = item.subMenu
        if (subMenu != null && visibleItems(subMenu).isNotEmpty()) {
            level.selectedItemId = item.itemId
            dismissCurrentLevel()
            levels.add(MenuLevel(subMenu))
            showCurrentLevel()
        } else {
            val expanded = item.expandActionView()
            close()
            if (!expanded) level.menu.performIdentifierAction(item.itemId, 0)
        }
    }

    private fun returnToParent() {
        dismissCurrentLevel()
        if (levels.isNotEmpty()) levels.removeAt(levels.lastIndex)
        showCurrentLevel()
    }

    private fun dismissCurrentLevel() {
        val dialog = currentDialog
        currentDialog = null
        disposeCurrentWindow?.invoke()
        disposeCurrentWindow = null
        dialog?.dismiss()
    }

    fun close() {
        levels.clear()
        dismissCurrentLevel()
    }

    private fun visibleItems(menu: Menu): List<MenuItem> =
        (0 until menu.size()).map(menu::getItem).filter { it.isVisible }

    private fun prioritize(items: List<MenuItem>, preferredItemIds: List<Int>): List<MenuItem> {
        val priorities = preferredItemIds.distinct().withIndex().associate { it.value to it.index }
        return items.sortedBy { priorities[it.itemId] ?: Int.MAX_VALUE }
    }

    private class MenuLevel(
        val menu: Menu,
        val preferredItemIds: List<Int> = emptyList(),
    ) {
        var selectedItemId: Int? = null
    }

    private class MenuItemAdapter(context: Context, private val items: List<MenuItem>) : BaseAdapter() {
        private val inflater = LayoutInflater.from(context)

        override fun getCount(): Int = items.size
        override fun getItem(position: Int): MenuItem = items[position]
        override fun getItemId(position: Int): Long = items[position].itemId.toLong()
        override fun areAllItemsEnabled(): Boolean = false
        override fun isEnabled(position: Int): Boolean = items[position].isEnabled && items[position].isVisible
        override fun getViewTypeCount(): Int = 2
        override fun getItemViewType(position: Int): Int =
            if (items[position].isCheckable || items[position].isChecked) CHECKABLE_ITEM else PLAIN_ITEM

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val item = items[position]
            val layout = if (getItemViewType(position) == CHECKABLE_ITEM) {
                android.R.layout.simple_list_item_multiple_choice
            } else {
                android.R.layout.simple_list_item_1
            }
            val view = convertView ?: inflater.inflate(layout, parent, false)
            (view as TextView).apply {
                text = item.title
                isEnabled = item.isEnabled
                if (this is CheckedTextView) isChecked = item.isChecked
            }
            return view
        }

        private companion object {
            const val PLAIN_ITEM = 0
            const val CHECKABLE_ITEM = 1
        }
    }
}
