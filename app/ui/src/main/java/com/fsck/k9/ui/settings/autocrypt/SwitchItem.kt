package com.fsck.k9.ui.settings.autocrypt

import android.view.View
import android.widget.Switch
import androidx.recyclerview.widget.RecyclerView
import com.fsck.k9.ui.R
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.mikepenz.fastadapter.listeners.ClickEventHook
import kotlinx.android.extensions.LayoutContainer

abstract class SwitchItem(private val id: Long) : AbstractItem<SwitchItem, SwitchViewHolder>() {
    override fun getIdentifier(): Long = id

    override fun getViewHolder(view: View): SwitchViewHolder = SwitchViewHolder(view)

    override fun bindView(viewHolder: SwitchViewHolder, payloads: List<Any>) {
        super.bindView(viewHolder, payloads)
        viewHolder.switch.isChecked = isEnabled
    }
}

class SwitchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), LayoutContainer {
    val switch: Switch = itemView.findViewById(R.id.switchView)

    override val containerView = itemView
}

class SwitchClickEvent(val action: (position: Int, isSelected: Boolean) -> Unit) : ClickEventHook<SwitchItem>() {
    override fun onBind(viewHolder: RecyclerView.ViewHolder): View? {
        return if (viewHolder is SwitchViewHolder) viewHolder.switch else null
    }

    override fun onClick(view: View, position: Int, fastAdapter: FastAdapter<SwitchItem>, item: SwitchItem) {
        action(position, !item.isSelected)
    }
}
