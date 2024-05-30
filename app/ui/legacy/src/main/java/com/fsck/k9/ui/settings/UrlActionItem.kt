package com.fsck.k9.ui.settings

import android.view.View
import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.fsck.k9.ui.R
import com.google.android.material.textview.MaterialTextView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem

internal class UrlActionItem(
    override var identifier: Long,
    val text: String,
    val url: String,
    @DrawableRes val icon: Int,
) : AbstractItem<UrlActionItem.ViewHolder>() {
    override val type = R.id.settings_list_url_item

    override val layoutRes = R.layout.text_icon_list_item

    override fun getViewHolder(v: View) = ViewHolder(v)

    class ViewHolder(view: View) : FastAdapter.ViewHolder<UrlActionItem>(view) {
        val text: MaterialTextView = view.findViewById(R.id.text)
        val icon: ImageView = view.findViewById(R.id.icon)

        override fun bindView(item: UrlActionItem, payloads: List<Any>) {
            text.text = item.text
            icon.setImageResource(item.icon)
        }

        override fun unbindView(item: UrlActionItem) {
            text.text = null
            icon.setImageDrawable(null)
        }
    }
}
