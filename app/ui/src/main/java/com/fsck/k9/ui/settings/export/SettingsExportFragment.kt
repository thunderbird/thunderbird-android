package com.fsck.k9.ui.settings.export

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.fsck.k9.ui.R
import com.fsck.k9.ui.observeNotNull
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter
import kotlinx.android.synthetic.main.fragment_settings_export.*
import org.koin.android.architecture.ext.viewModel


class SettingsExportFragment : Fragment() {
    private val viewModel: SettingsExportViewModel by viewModel()

    private lateinit var settingsExportAdapter: FastItemAdapter<CheckBoxItem>


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings_export, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            viewModel.initializeFromSavedState(savedInstanceState)
        }

        initializeSettingsExportList(view)
        exportButton.setOnClickListener { viewModel.onExportButtonClicked() }
        shareButton.setOnClickListener { viewModel.onShareButtonClicked() }

        viewModel.getUiModel().observeNotNull(this) { updateUi(it) }
        viewModel.getActionEvents().observeNotNull(this) { handleActionEvents(it) }
    }

    private fun initializeSettingsExportList(view: View) {
        settingsExportAdapter = FastItemAdapter<CheckBoxItem>().apply {
            setHasStableIds(true)
            withOnClickListener { _, _, item: CheckBoxItem, position ->
                viewModel.onSettingsListItemSelected(position, !item.isSelected)
                true
            }
            withEventHook(CheckBoxClickEvent { position, isSelected ->
                viewModel.onSettingsListItemSelected(position, isSelected)
            })
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.settingsExportList)
        recyclerView.adapter = settingsExportAdapter
    }

    private fun updateUi(model: SettingsExportUiModel) {
        when (model.exportButton) {
            ButtonState.DISABLED -> {
                exportButton.visibility = View.VISIBLE
                exportButton.isEnabled = false
            }
            ButtonState.ENABLED -> {
                exportButton.visibility = View.VISIBLE
                exportButton.isEnabled = true
            }
            ButtonState.INVISIBLE -> exportButton.visibility = View.INVISIBLE
            ButtonState.GONE -> exportButton.visibility = View.GONE
        }

        shareButton.visibility = if (model.isShareButtonVisible) View.VISIBLE else View.GONE
        progressBar.visibility = if (model.isProgressVisible) View.VISIBLE else View.GONE

        when (model.statusText) {
            StatusText.HIDDEN -> statusText.visibility = View.GONE
            StatusText.EXPORT_SUCCESS -> {
                statusText.visibility = View.VISIBLE
                statusText.text = getString(R.string.settings_export_success_generic)
            }
            StatusText.PROGRESS -> {
                statusText.visibility = View.VISIBLE
                statusText.text = getString(R.string.settings_export_progress_text)
            }
            StatusText.EXPORT_FAILURE -> {
                statusText.visibility = View.VISIBLE
                statusText.text = getString(R.string.settings_export_failure)
            }
        }

        setSettingsList(model.settingsList, model.isSettingsListEnabled)
    }

    //TODO: Update list instead of replacing it completely
    private fun setSettingsList(items: List<SettingsListItem>, enable: Boolean) {
        val checkBoxItems = items.map { item ->
            val checkBoxItem = when (item) {
                is SettingsListItem.GeneralSettings -> GeneralSettingsItem()
                is SettingsListItem.Account -> AccountItem(item)
            }

            checkBoxItem
                    .withSetSelected(item.selected)
                    .withEnabled(enable)
        }

        settingsExportAdapter.set(checkBoxItems)

        settingsExportList.isEnabled = enable
    }

    private fun handleActionEvents(action: Action) {
        when (action) {
            is Action.PickDocument -> pickDocument(action.fileNameSuggestion, action.mimeType)
            is Action.ShareDocument -> shareDocument(action.contentUri, action.mimeType)
        }
    }

    private fun pickDocument(fileNameSuggestion: String, mimeType: String) {
        val createDocumentIntent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            type = mimeType
            putExtra(Intent.EXTRA_TITLE, fileNameSuggestion)
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(createDocumentIntent, RESULT_PICK_DOCUMENT)
    }

    private fun shareDocument(contentUri: Uri, mimeType: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, null))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.saveInstanceState(outState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RESULT_PICK_DOCUMENT) {
            val contentUri = data?.data
            if (resultCode == Activity.RESULT_OK && contentUri != null) {
                viewModel.onDocumentPicked(contentUri)
            } else {
                viewModel.onDocumentPickCanceled()
            }
        }
    }


    companion object {
        private const val RESULT_PICK_DOCUMENT = Activity.RESULT_FIRST_USER
    }
}
