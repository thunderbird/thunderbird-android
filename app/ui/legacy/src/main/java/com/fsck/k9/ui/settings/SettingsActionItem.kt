package com.fsck.k9.ui.settings

import android.util.TypedValue
import androidx.annotation.IdRes
import com.fsck.k9.ui.R
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import com.xwray.groupie.kotlinandroidextensions.Item
import kotlinx.android.synthetic.main.text_icon_list_item.*

internal class SettingsActionItem(val text: String, @IdRes val navigationAction: Int, val icon: Int) : Item() {

    override fun getLayout(): Int = R.layout.text_icon_list_item

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.text.text = text

        val outValue = TypedValue()
        viewHolder.icon.context.theme.resolveAttribute(icon, outValue, true)
        viewHolder.icon.setImageResource(outValue.resourceId)
    }
}
