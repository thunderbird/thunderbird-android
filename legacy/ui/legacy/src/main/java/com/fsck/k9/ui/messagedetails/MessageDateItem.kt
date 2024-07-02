package com.fsck.k9.ui.messagedetails

import android.view.View
import com.fsck.k9.ui.R
import com.google.android.material.textview.MaterialTextView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem

internal class MessageDateItem(private val date: String) : AbstractItem<MessageDateItem.ViewHolder>() {
    override val type: Int = R.id.message_details_date
    override val layoutRes = R.layout.message_details_date_item

    override fun getViewHolder(v: View) = ViewHolder(v)

    class ViewHolder(view: View) : FastAdapter.ViewHolder<MessageDateItem>(view) {
        private val textView = view.findViewById<MaterialTextView>(R.id.date)

        override fun bindView(item: MessageDateItem, payloads: List<Any>) {
            textView.text = item.date
        }

        override fun unbindView(item: MessageDateItem) {
            textView.text = null
        }
    }
}
