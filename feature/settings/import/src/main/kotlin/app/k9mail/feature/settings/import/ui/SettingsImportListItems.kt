package app.k9mail.feature.settings.import.ui

import android.view.View
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import app.k9mail.feature.settings.importing.R
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textview.MaterialTextView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.mikepenz.fastadapter.listeners.ClickEventHook

private const val GENERAL_SETTINGS_ID = 0L
private const val ACCOUNT_ITEMS_ID_OFFSET = 1L

internal abstract class ImportListItem<VH : ImportCheckBoxViewHolder>(
    override var identifier: Long,
    private val importStatus: ImportStatus,
) : AbstractItem<VH>() {

    override fun bindView(holder: VH, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.checkBox.isChecked = isSelected
        holder.itemView.isEnabled = isEnabled
        holder.checkBox.isEnabled = isEnabled

        holder.checkBox.isVisible = importStatus == ImportStatus.NOT_AVAILABLE
        holder.statusIcon.isVisible = importStatus != ImportStatus.NOT_AVAILABLE

        if (importStatus != ImportStatus.NOT_AVAILABLE) {
            val imageLevel = when (importStatus) {
                ImportStatus.IMPORT_SUCCESS -> 0
                ImportStatus.IMPORT_SUCCESS_PASSWORD_REQUIRED -> 1
                ImportStatus.IMPORT_SUCCESS_AUTHORIZATION_REQUIRED -> 2
                ImportStatus.NOT_SELECTED -> 3
                ImportStatus.IMPORT_FAILURE -> 4
                else -> error("Unexpected import status: $importStatus")
            }
            holder.statusIcon.setImageLevel(imageLevel)

            val contentDescriptionStringResId = when (importStatus) {
                ImportStatus.IMPORT_SUCCESS -> R.string.settings_import_status_success
                ImportStatus.IMPORT_SUCCESS_PASSWORD_REQUIRED -> R.string.settings_import_status_password_required
                ImportStatus.IMPORT_SUCCESS_AUTHORIZATION_REQUIRED -> R.string.settings_import_status_log_in_required
                ImportStatus.NOT_SELECTED -> R.string.settings_import_status_not_imported
                ImportStatus.IMPORT_FAILURE -> R.string.settings_import_status_error
                else -> error("Unexpected import status: $importStatus")
            }
            val context = holder.statusIcon.context
            holder.statusIcon.contentDescription = context.getString(contentDescriptionStringResId)
        }
    }
}

internal open class ImportCheckBoxViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val checkBox: MaterialCheckBox = itemView.findViewById(R.id.checkBox)
    val statusIcon: ImageView = itemView.findViewById(R.id.statusIcon)
}

internal class ImportListItemClickEvent(val action: (position: Int) -> Unit) : ClickEventHook<ImportListItem<*>>() {

    override fun onBind(viewHolder: RecyclerView.ViewHolder): View? {
        return if (viewHolder is ImportCheckBoxViewHolder) viewHolder.checkBox else null
    }

    override fun onClick(
        v: View,
        position: Int,
        fastAdapter: FastAdapter<ImportListItem<*>>,
        item: ImportListItem<*>,
    ) {
        action(position)
    }
}

internal class GeneralSettingsItem(importStatus: ImportStatus) :
    ImportListItem<ImportCheckBoxViewHolder>(GENERAL_SETTINGS_ID, importStatus) {

    override val type = R.id.settings_import_list_general_item
    override val layoutRes = R.layout.settings_import_general_list_item

    override fun getViewHolder(v: View) = ImportCheckBoxViewHolder(v)
}

internal class AccountViewHolder(view: View) : ImportCheckBoxViewHolder(view) {
    val accountDisplayName: MaterialTextView = view.findViewById(R.id.accountDisplayName)
}

internal class AccountItem(account: SettingsListItem.Account) :
    ImportListItem<AccountViewHolder>(account.accountIndex + ACCOUNT_ITEMS_ID_OFFSET, account.importStatus) {

    private val displayName = account.displayName

    override val type = R.id.settings_import_list_account_item
    override val layoutRes = R.layout.settings_import_account_list_item

    override fun getViewHolder(v: View) = AccountViewHolder(v)

    override fun bindView(holder: AccountViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.accountDisplayName.text = displayName
    }
}
