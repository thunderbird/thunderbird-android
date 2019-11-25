package com.fsck.k9.ui.settings.encryption

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.fsck.k9.ui.R
import com.fsck.k9.ui.observeNotNull
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter
import org.koin.android.architecture.ext.viewModel


class SettingsEncryptionFragment : Fragment() {
    private val viewModel: SettingsEncryptionViewModel by viewModel()

    private lateinit var settingsEncryptionAdapter: FastItemAdapter<EncryptionSwitchItem>


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings_encryption, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        if (savedInstanceState != null) {
//            viewModel.initializeFromSavedState(savedInstanceState)
//        }

        initializeSettingsEncryptionList(view)

        viewModel.getUiModel().observeNotNull(this) { updateUi(it) }
    }

    private fun initializeSettingsEncryptionList(view: View) {
        settingsEncryptionAdapter = FastItemAdapter<EncryptionSwitchItem>().apply {
            setHasStableIds(true)
            withOnClickListener { _, _, item: EncryptionSwitchItem, position ->
                //                viewModel.onSettingsListItemSelected(position, !item.isSelected)
                true
            }
            withEventHook(SwitchClickEvent { position, isSelected ->
                //                viewModel.onSettingsListItemSelected(position, isSelected)
            })
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.settingsEncryptionList)
        recyclerView.adapter = settingsEncryptionAdapter
    }

    private fun updateUi(model: SettingsEncryptionUiModel) {
        setSettingsList(model.settingsList, model.isSettingsListEnabled)
    }

    private fun setSettingsList(items: List<SettingsListItem>, enable: Boolean) {
        val switchItems = items.map { item ->
            val switchItem = when (item) {
                is SettingsListItem.AdvancedSettings -> AdvancedSettingsItem()
                is SettingsListItem.EncryptionIdentity -> EncryptionIdentityItem(item)
            }

            switchItem.withEnabled(enable)
        }

        settingsEncryptionAdapter.set(switchItems)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.saveInstanceState(outState)
    }

}
