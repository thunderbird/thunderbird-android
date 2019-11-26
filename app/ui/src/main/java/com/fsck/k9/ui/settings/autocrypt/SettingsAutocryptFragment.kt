package com.fsck.k9.ui.settings.autocrypt

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


class SettingsAutocryptFragment : Fragment() {
    private val viewModel: SettingsAutocryptViewModel by viewModel()

    private lateinit var settingsAutocryptAdapter: FastItemAdapter<SwitchItem>


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings_autocrypt, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        if (savedInstanceState != null) {
//            viewModel.initializeFromSavedState(savedInstanceState)
//        }

        initializeSettingsIdentityList(view)

        viewModel.getUiModel().observeNotNull(this) { updateUi(it) }
    }

    private fun initializeSettingsIdentityList(view: View) {
        settingsAutocryptAdapter = FastItemAdapter<SwitchItem>().apply {
            setHasStableIds(true)
            withOnClickListener { _, _, item: SwitchItem, position ->
                //                viewModel.onSettingsListItemSelected(position, !item.isSelected)
                true
            }
            withEventHook(SwitchClickEvent { position, isSelected ->
                //                viewModel.onSettingsListItemSelected(position, isSelected)
            })
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.settingsEncryptionList)
        recyclerView.adapter = settingsAutocryptAdapter
    }

    private fun updateUi(model: SettingsAutocryptUiModel) {
        setSettingsList(model.settingsList, model.isSettingsListEnabled)
    }

    private fun setSettingsList(items: List<SettingsListItem>, enable: Boolean) {
        val switchItems = items.map { item ->
            val switchItem = when (item) {
                is SettingsListItem.AdvancedSettings -> AdvancedSettingsItem()
                is SettingsListItem.AutocryptIdentity -> AutocryptIdentityItem(item)
            }

            switchItem.withEnabled(enable)
        }

        settingsAutocryptAdapter.set(switchItems)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.saveInstanceState(outState)
    }

}
