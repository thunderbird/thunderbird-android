package com.fsck.k9.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.fsck.k9.Account
import com.fsck.k9.activity.setup.WelcomeMessage
import com.fsck.k9.ui.R
import com.fsck.k9.ui.observeNotNull
import com.fsck.k9.ui.settings.account.AccountSettingsActivity
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.Section
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.fragment_settings_list.*
import org.koin.android.architecture.ext.viewModel

class SettingsListFragment : Fragment() {
    private val viewModel: SettingsViewModel by viewModel()

    private lateinit var settingsAdapter: GroupAdapter<ViewHolder>


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initializeSettingsList()
        populateSettingsList()
    }

    private fun initializeSettingsList() {
        settingsAdapter = GroupAdapter()
        settingsAdapter.setOnItemClickListener { item, _ ->
            handleItemClick(item)
        }

        with(settings_list) {
            adapter = settingsAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun populateSettingsList() {
        viewModel.accounts.observeNotNull(this) { accounts ->
            if (accounts.isEmpty()) {
                launchWelcomeScreen()
            } else {
                populateSettingsList(accounts)
            }
        }
    }

    private fun populateSettingsList(accounts: List<Account>) {
        settingsAdapter.clear()

        val miscSection = Section().apply {
            val item = SettingsActionItem(getString(R.string.about_action), SettingsAction.ABOUT_SCREEN, R.attr.iconSettingsAbout)
            add(item)
        }
        settingsAdapter.add(miscSection)

        val generalSection = Section().apply {
            val item = SettingsActionItem(getString(R.string.general_settings_title),
                    SettingsAction.GENERAL_SETTINGS, R.attr.iconSettingsGeneral)
            add(item)
        }
        settingsAdapter.add(generalSection)

        val accountSection = Section().apply {
            for (account in accounts) {
                add(AccountItem(account))
            }
            val item = SettingsActionItem(getString(R.string.add_account_action), SettingsAction.ADD_ACCOUNT, R.attr.iconSettingsAccountAdd)
            add(item)
        }
        accountSection.setHeader(SettingsDividerItem(getString(R.string.accounts_title)))
        settingsAdapter.add(accountSection)
    }

    private fun handleItemClick(item: Item<*>) {
        when (item) {
            is AccountItem -> launchAccountSettings(item.account)
            is SettingsActionItem -> item.action.execute(requireActivity())
        }
    }

    private fun launchAccountSettings(account: Account) {
        AccountSettingsActivity.start(requireActivity(), account.uuid)
    }

    private fun launchWelcomeScreen() {
        val activity = requireActivity()
        WelcomeMessage.showWelcomeMessage(activity)
        activity.finish()
    }
}
