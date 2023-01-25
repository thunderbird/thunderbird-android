package com.fsck.k9.ui.messagedetails

import android.view.View
import com.fsck.k9.ui.R
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem

internal class MessageDetailsDividerItem : AbstractItem<MessageDetailsDividerItem.ViewHolder>() {
    override val type: Int = R.id.message_details_divider
    override val layoutRes = R.layout.message_details_divider_item

    override fun getViewHolder(v: View) = ViewHolder(v)

    class ViewHolder(view: View) : FastAdapter.ViewHolder<MessageDetailsDividerItem>(view) {
        override fun bindView(item: MessageDetailsDividerItem, payloads: List<Any>) = Unit

        override fun unbindView(item: MessageDetailsDividerItem) = Unit
    }
}
