package com.fsck.k9.ui.settings.export

import android.view.View
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView
import com.fsck.k9.ui.R
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.mikepenz.fastadapter.listeners.ClickEventHook
import kotlinx.android.extensions.LayoutContainer

abstract class CheckBoxItem(override var identifier: Long) : AbstractItem<CheckBoxViewHolder>() {
    override fun getViewHolder(v: View): CheckBoxViewHolder = CheckBoxViewHolder(v)

    override fun bindView(holder: CheckBoxViewHolder, payloads: MutableList<Any>) {
        super.bindView(holder, payloads)
        holder.checkBox.isChecked = isSelected
        holder.itemView.isEnabled = isEnabled
        holder.checkBox.isEnabled = isEnabled
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

    override fun onClick(v: View, position: Int, fastAdapter: FastAdapter<CheckBoxItem>, item: CheckBoxItem) {
        action(position, !item.isSelected)
    }
}
