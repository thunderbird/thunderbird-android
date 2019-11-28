package com.fsck.k9.ui.settings.import

import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.fsck.k9.ui.R
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.mikepenz.fastadapter.listeners.ClickEventHook
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.settings_import_account_list_item.*

private const val GENERAL_SETTINGS_ID = 0L
private const val ACCOUNT_ITEMS_ID_OFFSET = 1L

abstract class ImportListItem(private val id: Long, private val importStatus: ImportStatus)
    : AbstractItem<ImportListItem, ImportCheckBoxViewHolder>() {

    override fun getIdentifier(): Long = id

    override fun getViewHolder(view: View): ImportCheckBoxViewHolder {
        return ImportCheckBoxViewHolder(view)
    }

    override fun bindView(viewHolder: ImportCheckBoxViewHolder, payloads: List<Any>) {
        super.bindView(viewHolder, payloads)
        viewHolder.checkBox.isChecked = isSelected
        viewHolder.itemView.isEnabled = isEnabled
        viewHolder.checkBox.isEnabled = isEnabled

        viewHolder.checkBox.isVisible = importStatus == ImportStatus.NOT_AVAILABLE
        viewHolder.statusIcon.isVisible = importStatus != ImportStatus.NOT_AVAILABLE

        if (importStatus != ImportStatus.NOT_AVAILABLE) {
            val imageLevel = when (importStatus) {
                ImportStatus.IMPORT_SUCCESS -> 0
                ImportStatus.IMPORT_SUCCESS_PASSWORD_REQUIRED -> 1
                ImportStatus.NOT_SELECTED -> 2
                ImportStatus.IMPORT_FAILURE -> 3
                else -> error("Unexpected import status: $importStatus")
            }
            viewHolder.statusIcon.setImageLevel(imageLevel)

            val contentDescriptionStringResId = when (importStatus) {
                ImportStatus.IMPORT_SUCCESS -> R.string.settings_import_status_success
                ImportStatus.IMPORT_SUCCESS_PASSWORD_REQUIRED -> R.string.settings_import_status_password_required
                ImportStatus.NOT_SELECTED -> R.string.settings_import_status_not_imported
                ImportStatus.IMPORT_FAILURE -> R.string.settings_import_status_error
                else -> error("Unexpected import status: $importStatus")
            }
            val context = viewHolder.containerView.context
            viewHolder.statusIcon.contentDescription = context.getString(contentDescriptionStringResId)
        }
    }
}

class ImportCheckBoxViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), LayoutContainer {
    val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)
    val statusIcon: ImageView = itemView.findViewById(R.id.statusIcon)

    override val containerView = itemView
}

class ImportListItemClickEvent(val action: (position: Int) -> Unit) : ClickEventHook<ImportListItem>() {

    override fun onBind(viewHolder: RecyclerView.ViewHolder): View? {
        return if (viewHolder is ImportCheckBoxViewHolder) viewHolder.checkBox else null
    }

    override fun onClick(
            view: View,
            position: Int,
            fastAdapter: FastAdapter<ImportListItem>,
            item: ImportListItem
    ) {
        action(position)
    }
}

class GeneralSettingsItem(importStatus: ImportStatus) : ImportListItem(GENERAL_SETTINGS_ID, importStatus) {

    override fun getType(): Int = R.id.settings_import_list_general_item

    override fun getLayoutRes(): Int = R.layout.settings_import_general_list_item
}

class AccountItem(account: SettingsListItem.Account)
    : ImportListItem(account.accountIndex + ACCOUNT_ITEMS_ID_OFFSET, account.importStatus) {

    private val displayName = account.displayName

    override fun getType(): Int = R.id.settings_import_list_account_item

    override fun getLayoutRes(): Int = R.layout.settings_import_account_list_item

    override fun bindView(viewHolder: ImportCheckBoxViewHolder, payloads: List<Any>) {
        super.bindView(viewHolder, payloads)
        viewHolder.accountDisplayName.text = displayName
    }
}
