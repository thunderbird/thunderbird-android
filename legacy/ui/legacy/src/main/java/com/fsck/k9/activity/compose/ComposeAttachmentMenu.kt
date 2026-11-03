package com.fsck.k9.activity.compose

import android.app.Activity
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import com.fsck.k9.ui.R
import com.fsck.k9.view.PopupKeypadBackNavigation
import com.fsck.k9.view.PopupListInitialSelection
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.thunderbird.core.android.common.keypad.KeypadDialogNavigation
import net.thunderbird.core.android.common.keypad.KeypadWindowInstaller

/** Reuses the existing attachment menu actions in an independently navigable keypad window. */
internal object ComposeAttachmentMenu {
    fun show(
        activity: Activity,
        menu: Menu,
        installer: KeypadWindowInstaller,
        isEnabled: () -> Boolean,
    ): AlertDialog {
        val items = (0 until menu.size()).map(menu::getItem).filter { it.isVisible }
        val builder = MaterialAlertDialogBuilder(activity)
        val adapter = object : ArrayAdapter<CharSequence>(
            builder.context,
            android.R.layout.simple_list_item_1,
            items.map { it.title ?: "" },
        ) {
            override fun areAllItemsEnabled(): Boolean = false
            override fun isEnabled(position: Int): Boolean = items[position].isEnabled
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
                super.getView(position, convertView, parent).apply { this.isEnabled = items[position].isEnabled }
        }
        val dialog = builder
            .setTitle(R.string.add_attachment_action)
            .setAdapter(adapter) { _, position ->
                val item = items[position]
                if (item.isEnabled) menu.performIdentifierAction(item.itemId, 0)
            }
            .create()
        KeypadDialogNavigation.bind(dialog, installer).show()
        dialog.listView.apply {
            setOnKeyListener(PopupKeypadBackNavigation(isEnabled) { dialog.cancel() })
            val firstEnabled = items.indexOfFirst { it.isEnabled }
            if (firstEnabled >= 0) {
                PopupListInitialSelection(this, firstEnabled) { dialog.isShowing && isEnabled() }
            }
        }
        return dialog
    }
}
