package com.fsck.k9.ui.settings

import android.view.View
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import com.fsck.k9.ui.R
import com.google.android.material.textview.MaterialTextView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem

@Suppress("detekt.UnnecessaryAnnotationUseSiteTarget") // https://github.com/detekt/detekt/issues/8212
internal class SettingsActionItem(
    override var identifier: Long,
    val text: String,
    @param:IdRes val navigationAction: Int,
    @param:DrawableRes val icon: Int,
) : AbstractItem<SettingsActionItem.ViewHolder>() {
    override val type = R.id.settings_list_action_item

    override val layoutRes = R.layout.text_icon_list_item

    override fun getViewHolder(v: View) = ViewHolder(v)

    class ViewHolder(view: View) : FastAdapter.ViewHolder<SettingsActionItem>(view) {
        val text: MaterialTextView = view.findViewById(R.id.text)
        val icon: ImageView = view.findViewById(R.id.icon)

        override fun bindView(item: SettingsActionItem, payloads: List<Any>) {
            text.text = item.text
            icon.setImageResource(item.icon)
        }

        override fun unbindView(item: SettingsActionItem) {
            text.text = null
            icon.setImageDrawable(null)
        }
    }
}
