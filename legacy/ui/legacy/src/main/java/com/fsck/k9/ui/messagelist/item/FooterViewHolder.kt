package com.fsck.k9.ui.messagelist.item

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.fsck.k9.ui.R
import com.google.android.material.textview.MaterialTextView

class FooterViewHolder(view: View) : MessageListViewHolder(view) {
    val textView: MaterialTextView = view.findViewById(R.id.main_text)

    fun bind(listItem: String?) {
        textView.text = listItem
    }

    companion object {
        fun create(
            layoutInflater: LayoutInflater,
            parent: ViewGroup,
            onClickListener: View.OnClickListener,
        ): FooterViewHolder {
            val view = layoutInflater.inflate(R.layout.message_list_item_footer, parent, false)
            view.setOnClickListener(onClickListener)
            return FooterViewHolder(view)
        }
    }
}
