package com.fsck.k9.ui.settings

import com.fsck.k9.ui.R
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import com.xwray.groupie.kotlinandroidextensions.Item
import kotlinx.android.synthetic.main.text_divider_list_item.*

internal class SettingsDividerItem(val text: String) : Item() {
    override fun getLayout(): Int = R.layout.text_divider_list_item

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.text.text = text
    }
}
