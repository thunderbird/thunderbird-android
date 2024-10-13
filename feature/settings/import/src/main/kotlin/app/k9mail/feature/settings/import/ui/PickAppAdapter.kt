package app.k9mail.feature.settings.import.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.view.isVisible
import app.k9mail.feature.settings.importing.R

internal class PickAppAdapter(context: Context) : ArrayAdapter<AppInfo>(context, 0) {
    private val inflater = LayoutInflater.from(context)

    private fun getItemOrThrow(position: Int): AppInfo {
        return getItem(position) ?: error("Item at position $position not found")
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.settings_import_pick_app_list_item, parent, false)

        val appNameTextView = view.findViewById<TextView>(R.id.settings_import_app_name)
        val appNoteTextView = view.findViewById<View>(R.id.settings_import_app_note)

        val item = getItemOrThrow(position)
        appNameTextView.text = item.appName
        appNoteTextView.isVisible = !item.isImportSupported

        view.isEnabled = item.isImportSupported

        return view
    }

    override fun areAllItemsEnabled(): Boolean {
        return false
    }

    override fun isEnabled(position: Int): Boolean {
        return getItemOrThrow(position).isImportSupported
    }
}
