package com.fsck.k9.ui.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.MenuItem
import com.fsck.k9.Account
import com.fsck.k9.activity.K9Activity
import com.fsck.k9.activity.setup.WelcomeMessage
import com.fsck.k9.ui.R
import com.fsck.k9.ui.observeNotNull
import com.fsck.k9.ui.settings.account.AccountSettingsActivity
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.Section
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.activity_settings.*
import org.koin.android.architecture.ext.viewModel

class SettingsActivity : K9Activity() {
    private val viewModel: SettingsViewModel by viewModel()

    private lateinit var settingsAdapter: GroupAdapter<ViewHolder>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLayout(R.layout.activity_settings)

        initializeActionBar()
        initializeSettingsList()

        populateSettingsList()
    }

    private fun initializeActionBar() {
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    private fun initializeSettingsList() {
        settingsAdapter = GroupAdapter()
        settingsAdapter.setOnItemClickListener { item, _ ->
            handleItemClick(item)
        }

        with(settings_list) {
            adapter = settingsAdapter
            layoutManager = LinearLayoutManager(this@SettingsActivity)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun handleItemClick(item: Item<*>) {
        when (item) {
            is AccountItem -> launchAccountSettings(item.account)
            is SettingsActionItem -> item.action.execute(this)
        }
    }

    private fun launchAccountSettings(account: Account) {
        AccountSettingsActivity.start(this, account.uuid)
    }

    private fun launchWelcomeScreen() {
        WelcomeMessage.showWelcomeMessage(this)
        finish()
    }


    companion object {
        @JvmStatic fun launch(activity: Activity) {
            val intent = Intent(activity, SettingsActivity::class.java)
            activity.startActivity(intent)
        }
    }
}
