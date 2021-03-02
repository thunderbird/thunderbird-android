package com.fsck.k9.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fsck.k9.Account
import com.fsck.k9.ui.R
import com.fsck.k9.ui.observeNotNull
import com.fsck.k9.ui.settings.account.AccountSettingsActivity
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsListFragment : Fragment() {
    private val viewModel: SettingsViewModel by viewModel()

    private lateinit var itemAdapter: ItemAdapter<GenericItem>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initializeSettingsList(recyclerView = view.findViewById(R.id.settings_list))
        populateSettingsList()
    }

    private fun initializeSettingsList(recyclerView: RecyclerView) {
        itemAdapter = ItemAdapter()

        val settingsListAdapter = FastAdapter.with(itemAdapter).apply {
            setHasStableIds(true)
            onClickListener = { _, _, item, _ ->
                handleItemClick(item)
                true
            }
        }

        with(recyclerView) {
            adapter = settingsListAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun populateSettingsList() {
        viewModel.accounts.observeNotNull(this) { accounts ->
            val accountsFinishedSetup = accounts.filter { it.isFinishedSetup }
            if (accountsFinishedSetup.isEmpty()) {
                launchOnboarding()
            } else {
                populateSettingsList(accountsFinishedSetup)
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

                addUrlAction(
                    text = getString(R.string.user_forum_title),
                    url = getString(R.string.user_forum_url),
                    icon = R.attr.iconUserForum
                )
            }
        }

        itemAdapter.setNewList(listItems)
    }

    private fun handleItemClick(item: GenericItem) {
        when (item) {
            is AccountItem -> launchAccountSettings(item.account)
            is UrlActionItem -> openUrl(item.url)
            is SettingsActionItem -> findNavController().navigate(item.navigationAction)
        }
    }

    private fun openUrl(url: String) {
        try {
            val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(viewIntent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), R.string.error_activity_not_found, Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchAccountSettings(account: Account) {
        AccountSettingsActivity.start(requireActivity(), account.uuid)
    }

    private fun launchOnboarding() {
        findNavController().navigate(R.id.action_settingsListScreen_to_onboardingScreen)
        requireActivity().finishAffinity()
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

        fun addUrlAction(text: String, url: String, @AttrRes icon: Int) {
            itemId++
            settingsList.add(UrlActionItem(itemId, text, url, icon))
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
