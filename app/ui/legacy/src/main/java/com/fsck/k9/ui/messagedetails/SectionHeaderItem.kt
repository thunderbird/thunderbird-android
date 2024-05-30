package com.fsck.k9.ui.messagedetails

import android.view.View
import com.fsck.k9.ui.R
import com.google.android.material.textview.MaterialTextView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem

internal class SectionHeaderItem(
    private val title: String,
    private val extra: String?,
) : AbstractItem<SectionHeaderItem.ViewHolder>() {
    override val type: Int = R.id.message_details_section_header
    override val layoutRes = R.layout.message_details_section_header_item

    override fun getViewHolder(v: View) = ViewHolder(v)

    class ViewHolder(view: View) : FastAdapter.ViewHolder<SectionHeaderItem>(view) {
        private val textView = view.findViewById<MaterialTextView>(R.id.title)
        private val extraTextView = view.findViewById<MaterialTextView>(R.id.extra)

        override fun bindView(item: SectionHeaderItem, payloads: List<Any>) {
            textView.text = item.title
            extraTextView.text = item.extra
        }

        override fun unbindView(item: SectionHeaderItem) {
            textView.text = null
            extraTextView.text = null
        }
    }
}
