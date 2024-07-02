package com.fsck.k9.ui.settings.export

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.fsck.k9.ui.R
import com.google.android.material.checkbox.MaterialCheckBox
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.mikepenz.fastadapter.listeners.ClickEventHook

abstract class CheckBoxItem<VH : CheckBoxViewHolder>(override var identifier: Long) : AbstractItem<VH>() {
    override fun bindView(holder: VH, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.checkBox.isChecked = isSelected
        holder.itemView.isEnabled = isEnabled
        holder.checkBox.isEnabled = isEnabled
    }
}

open class CheckBoxViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val checkBox: MaterialCheckBox = itemView.findViewById(R.id.checkBox)
}

class CheckBoxClickEvent(val action: (position: Int, isSelected: Boolean) -> Unit) : ClickEventHook<CheckBoxItem<*>>() {
    override fun onBind(viewHolder: RecyclerView.ViewHolder): View? {
        return if (viewHolder is CheckBoxViewHolder) viewHolder.checkBox else null
    }

    override fun onClick(v: View, position: Int, fastAdapter: FastAdapter<CheckBoxItem<*>>, item: CheckBoxItem<*>) {
        action(position, !item.isSelected)
    }
}
