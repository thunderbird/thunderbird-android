package com.fsck.k9.ui.messagedetails

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.fsck.k9.ui.R
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem

internal class EmptyItem : AbstractItem<EmptyItem.ViewHolder>() {
    override val type: Int = R.id.message_details_empty
    override val layoutRes = 0

    override fun createView(ctx: Context, parent: ViewGroup?): View {
        return View(ctx)
    }

    override fun getViewHolder(v: View) = ViewHolder(v)

    class ViewHolder(view: View) : FastAdapter.ViewHolder<EmptyItem>(view) {
        override fun bindView(item: EmptyItem, payloads: List<Any>) = Unit

        override fun unbindView(item: EmptyItem) = Unit
    }
}
