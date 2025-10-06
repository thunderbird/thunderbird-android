package com.fsck.k9.ui.settings

import android.content.Intent
import android.view.View
import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.fsck.k9.ui.R
import com.google.android.material.textview.MaterialTextView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem

@Suppress("detekt.UnnecessaryAnnotationUseSiteTarget") // https://github.com/detekt/detekt/issues/8212
class IntentItem(
    override var identifier: Long,
    val text: String,
    @param:DrawableRes val icon: Int,
    val intent: Intent,
) : AbstractItem<IntentItem.ViewHolder>() {
    override val type = R.id.settings_list_intent_item

    override val layoutRes = R.layout.text_icon_list_item

    override fun getViewHolder(v: View) = ViewHolder(v)

    class ViewHolder(view: View) : FastAdapter.ViewHolder<IntentItem>(view) {
        val text: MaterialTextView = view.findViewById(R.id.text)
        val icon: ImageView = view.findViewById(R.id.icon)

        override fun bindView(item: IntentItem, payloads: List<Any>) {
            text.text = item.text
            icon.setImageResource(item.icon)
        }

        override fun unbindView(item: IntentItem) {
            text.text = null
            icon.setImageDrawable(null)
        }
    }
}
