package com.fsck.k9.ui.settings

import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.fsck.k9.ui.R
import com.google.android.material.textview.MaterialTextView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.drag.IDraggable
import com.mikepenz.fastadapter.items.AbstractItem
import com.mikepenz.fastadapter.listeners.TouchEventHook
import net.thunderbird.core.android.account.LegacyAccountDto

internal class AccountItem(
    val account: LegacyAccountDto,
    override var isDraggable: Boolean,
) : AbstractItem<AccountItem.ViewHolder>(), IDraggable {
    override var identifier = 200L + account.accountNumber

    override val type = R.id.settings_list_account_item

    override val layoutRes = R.layout.account_list_item

    override fun getViewHolder(v: View) = ViewHolder(v)

    class ViewHolder(view: View) : FastAdapter.ViewHolder<AccountItem>(view) {
        val name: MaterialTextView = view.findViewById(R.id.name)
        val email: MaterialTextView = view.findViewById(R.id.email)
        val dragHandle: ImageView = view.findViewById(R.id.drag_handle)

        override fun bindView(item: AccountItem, payloads: List<Any>) {
            val accountName = item.account.name
            if (accountName != null) {
                name.text = item.account.name
                email.text = item.account.email
                email.isVisible = true
            } else {
                name.text = item.account.email
                email.isVisible = false
            }

            dragHandle.isGone = !item.isDraggable
        }

        override fun unbindView(item: AccountItem) {
            name.text = null
            email.text = null
        }
    }
}

internal class DragHandleTouchEvent(val action: (position: Int) -> Unit) : TouchEventHook<AccountItem>() {
    override fun onBind(viewHolder: RecyclerView.ViewHolder): View? {
        return if (viewHolder is AccountItem.ViewHolder) viewHolder.dragHandle else null
    }

    override fun onTouch(
        v: View,
        event: MotionEvent,
        position: Int,
        fastAdapter: FastAdapter<AccountItem>,
        item: AccountItem,
    ): Boolean {
        return if (event.action == MotionEvent.ACTION_DOWN) {
            action(position)
            true
        } else {
            false
        }
    }
}
