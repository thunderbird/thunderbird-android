package com.fsck.k9.ui.settings

import android.view.View
import android.widget.TextView
import com.fsck.k9.ui.R
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem

internal class SettingsDividerItem(
    override var identifier: Long,
    val text: String
) : AbstractItem<SettingsDividerItem.ViewHolder>() {
    override val type = R.id.settings_list_header_item

    override val layoutRes = R.layout.text_divider_list_item

    override fun getViewHolder(v: View) = ViewHolder(v)

    class ViewHolder(view: View) : FastAdapter.ViewHolder<SettingsDividerItem>(view) {
        val text: TextView = view.findViewById(R.id.text)

        override fun bindView(item: SettingsDividerItem, payloads: List<Any>) {
            text.text = item.text
        }

        override fun unbindView(item: SettingsDividerItem) {
            text.text = null
        }
    }
}
