package com.fsck.k9.ui.settings.export

import android.view.View
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView
import com.fsck.k9.ui.R
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.mikepenz.fastadapter.listeners.ClickEventHook
import kotlinx.android.extensions.LayoutContainer

abstract class CheckBoxItem(private val id: Long) : AbstractItem<CheckBoxItem, CheckBoxViewHolder>() {
    override fun getIdentifier(): Long = id

    override fun getViewHolder(view: View): CheckBoxViewHolder = CheckBoxViewHolder(view)

    override fun bindView(viewHolder: CheckBoxViewHolder, payloads: List<Any>) {
        super.bindView(viewHolder, payloads)
        viewHolder.checkBox.isChecked = isSelected
        viewHolder.itemView.isEnabled = isEnabled
        viewHolder.checkBox.isEnabled = isEnabled
    }
}

class CheckBoxViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), LayoutContainer {
    val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)

    override val containerView = itemView
}

class CheckBoxClickEvent(val action: (position: Int, isSelected: Boolean) -> Unit) : ClickEventHook<CheckBoxItem>() {
    override fun onBind(viewHolder: RecyclerView.ViewHolder): View? {
        return if (viewHolder is CheckBoxViewHolder) viewHolder.checkBox else null
    }

    override fun onClick(view: View, position: Int, fastAdapter: FastAdapter<CheckBoxItem>, item: CheckBoxItem) {
        action(position, !item.isSelected)
    }
}
