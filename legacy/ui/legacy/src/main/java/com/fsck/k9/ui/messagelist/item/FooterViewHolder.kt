package com.fsck.k9.ui.messagelist.item

import android.view.View
import com.fsck.k9.ui.R
import com.google.android.material.textview.MaterialTextView

class FooterViewHolder(view: View) : MessageListViewHolder(view) {
    val text: MaterialTextView = view.findViewById(R.id.main_text)
}
