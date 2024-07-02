package com.fsck.k9.ui.managefolders

import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.fsck.k9.ui.R
import com.google.android.material.textview.MaterialTextView
import com.mikepenz.fastadapter.items.AbstractItem

class FolderListItem(
    val folderId: Long,
    private val folderIconResource: Int,
    val displayName: String,
) : AbstractItem<FolderListViewHolder>() {
    override var identifier: Long = folderId
    override val layoutRes = R.layout.folder_list_item
    override val type: Int = 1

    override fun getViewHolder(v: View) = FolderListViewHolder(v)

    override fun bindView(holder: FolderListViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.icon.setImageResource(folderIconResource)
        holder.name.text = displayName
    }
}

class FolderListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val icon: ImageView = itemView.findViewById(R.id.folder_icon)
    val name: MaterialTextView = itemView.findViewById(R.id.folder_name)
}
