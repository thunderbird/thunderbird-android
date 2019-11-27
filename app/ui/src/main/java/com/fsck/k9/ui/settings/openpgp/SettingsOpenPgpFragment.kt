package com.fsck.k9.ui.settings.openpgp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fsck.k9.Account
import com.fsck.k9.Identity
import com.fsck.k9.Preferences
import com.fsck.k9.ui.R
import com.fsck.k9.ui.observeNotNull
import com.fsck.k9.ui.settings.AccountItem
import com.fsck.k9.ui.settings.SettingsActionItem
import com.fsck.k9.ui.settings.SettingsDividerItem
import com.fsck.k9.ui.settings.account.AccountSettingsActivity
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.Section
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.fragment_settings_list.*
import org.koin.android.architecture.ext.viewModel
import org.koin.android.ext.android.inject

class SettingsOpenPgpFragment : Fragment() {
    private val viewModel: SettingsOpenPgpViewModel by viewModel()
    private val preferences: Preferences by inject()

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
            if (accounts.isNotEmpty()) {
                populateSettingsList(accounts)
            }
        }
    }

    private fun populateSettingsList(accounts: List<Account>) {
        settingsAdapter.clear()

        val accountsSection = Section()
        for (account in accounts) {
            val accountIdentitiesSection = Section().apply {
                for ((identityIndex, identity) in account.identities.withIndex()) {
                    add(IdentityItem(identity, object : IdentityItem.OnIdentityClickedListener {
                        override fun onIdentityClicked(item: IdentityItem, identity: Identity) {
                            onIdentityClicked(account, identityIndex, identity)
                        }

                        override fun onCheckedChange(item: IdentityItem, identity: Identity, checked: Boolean) {
                            onIdentityChecked(account, identityIndex, identity, checked)
                        }
                    }))
                }
            }
            accountsSection.add(accountIdentitiesSection)
        }
        settingsAdapter.add(accountsSection)

        val advancedSection = Section().apply {
            val generalSettingsActionItem = SettingsActionItem(
                    "TODO",
                    R.id.action_settingsListScreen_to_generalSettingsScreen,
                    R.attr.iconSettingsGeneral
            )
            add(generalSettingsActionItem)
        }
        advancedSection.setHeader(SettingsDividerItem(getString(R.string.advanced)))
        settingsAdapter.add(advancedSection)
    }

    private fun handleItemClick(item: Item<*>) {
        when (item) {
            is AccountItem -> launchAccountSettings(item.account)
            is SettingsActionItem -> findNavController().navigate(item.navigationAction)
        }
    }

    private fun launchAccountSettings(account: Account) {
        AccountSettingsActivity.start(requireActivity(), account.uuid)
    }

    private fun onIdentityClicked(account: Account, identityIndex: Int, identity: Identity) {
        Toast.makeText(requireActivity(), "identity: " + identity.email, Toast.LENGTH_SHORT).show();
    }

    private fun onIdentityChecked(account: Account, identityIndex: Int, identity: Identity, checked: Boolean) {
        account.identities[identityIndex] = identity.copy(openPgpEnabled = checked)
        preferences.saveAccount(account)

        Toast.makeText(requireActivity(), "identity: " + identity.email + "checked: " + checked, Toast.LENGTH_SHORT).show();
    }
}
