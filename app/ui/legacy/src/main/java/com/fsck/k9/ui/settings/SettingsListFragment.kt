package com.fsck.k9.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fsck.k9.Account
import com.fsck.k9.ui.R
import com.fsck.k9.ui.observeNotNull
import com.fsck.k9.ui.settings.account.AccountSettingsActivity
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import kotlinx.android.synthetic.main.fragment_settings_list.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsListFragment : Fragment() {
    private val viewModel: SettingsViewModel by viewModel()

    private lateinit var itemAdapter: ItemAdapter<GenericItem>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initializeSettingsList()
        populateSettingsList()
    }

    private fun initializeSettingsList() {
        itemAdapter = ItemAdapter()

        val settingsListAdapter = FastAdapter.with(itemAdapter).apply {
            setHasStableIds(true)
            onClickListener = { _, _, item, _ ->
                handleItemClick(item)
                true
            }
        }

        with(settings_list) {
            adapter = settingsListAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun populateSettingsList() {
        viewModel.accounts.observeNotNull(this) { accounts ->
            if (accounts.isEmpty()) {
                launchOnboarding()
            } else {
                populateSettingsList(accounts)
            }
        }
    }

    private fun populateSettingsList(accounts: List<Account>) {
        val listItems = buildSettingsList {
            addAction(
                text = getString(R.string.general_settings_title),
                navigationAction = R.id.action_settingsListScreen_to_generalSettingsScreen,
                icon = R.attr.iconSettingsGeneral
            )

            addSection(title = getString(R.string.accounts_title)) {
                for (account in accounts) {
                    addAccount(account)
                }

                addAction(
                    text = getString(R.string.add_account_action),
                    navigationAction = R.id.action_settingsListScreen_to_addAccountScreen,
                    icon = R.attr.iconSettingsAccountAdd
                )

                NewSetupUiHack.addAction(this)
            }

            addSection(title = getString(R.string.settings_list_backup_category)) {
                addAction(
                    text = getString(R.string.settings_export_title),
                    navigationAction = R.id.action_settingsListScreen_to_settingsExportScreen,
                    icon = R.attr.iconSettingsExport
                )

                addAction(
                    text = getString(R.string.settings_import_title),
                    navigationAction = R.id.action_settingsListScreen_to_settingsImportScreen,
                    icon = R.attr.iconSettingsImport
                )
            }

            addSection(title = getString(R.string.settings_list_miscellaneous_category)) {
                addAction(
                    text = getString(R.string.about_action),
                    navigationAction = R.id.action_settingsListScreen_to_aboutScreen,
                    icon = R.attr.iconSettingsAbout
                )
            }
        }

        itemAdapter.setNewList(listItems)
    }

    private fun handleItemClick(item: GenericItem) {
        when (item) {
            is AccountItem -> launchAccountSettings(item.account)
            is SettingsActionItem -> findNavController().navigate(item.navigationAction)
        }
    }

    private fun launchAccountSettings(account: Account) {
        AccountSettingsActivity.start(requireActivity(), account.uuid)
    }

    private fun launchOnboarding() {
        findNavController().navigate(R.id.action_settingsListScreen_to_onboardingScreen)
        requireActivity().finish()
    }

    private fun buildSettingsList(block: SettingsListBuilder.() -> Unit): List<GenericItem> {
        return SettingsListBuilder().apply(block).toList()
    }

    internal class SettingsListBuilder {
        private val settingsList = mutableListOf<GenericItem>()
        private var itemId = 0L

        fun addAction(text: String, @IdRes navigationAction: Int, @AttrRes icon: Int) {
            itemId++
            settingsList.add(SettingsActionItem(itemId, text, navigationAction, icon))
        }

        fun addAccount(account: Account) {
            settingsList.add(AccountItem(account))
        }

        fun addSection(title: String, block: SettingsListBuilder.() -> Unit) {
            itemId++
            settingsList.add(SettingsDividerItem(itemId, title))
            block()
        }

        fun toList(): List<GenericItem> = settingsList
    }
}
