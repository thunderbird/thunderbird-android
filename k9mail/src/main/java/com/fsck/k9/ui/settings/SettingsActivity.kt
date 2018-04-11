package com.fsck.k9.ui.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.MenuItem
import com.fsck.k9.Account
import com.fsck.k9.R
import com.fsck.k9.activity.K9Activity
import com.fsck.k9.activity.setup.AccountSettings
import com.fsck.k9.ui.observe
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
        setContentView(R.layout.activity_settings)

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
        viewModel.accounts.observe(this) { accounts ->
            populateSettingsList(accounts)
        }
    }

    private fun populateSettingsList(accounts: List<Account>) {
        settingsAdapter.clear()

        val generalSection = Section().apply {
            add(SettingsActionItem(getString(R.string.general_settings_title), SettingsAction.GENERAL_SETTINGS))
        }
        settingsAdapter.add(generalSection)

        val accountSection = Section().apply {
            for (account in accounts) {
                add(AccountItem(account))
            }
            add(SettingsActionItem(getString(R.string.add_account_action), SettingsAction.ADD_ACCOUNT))
        }
        settingsAdapter.add(accountSection)

        //TODO: add header and/or divider
        val miscSection = Section().apply {
            add(SettingsActionItem(getString(R.string.about_action), SettingsAction.ABOUT_SCREEN))
        }
        settingsAdapter.add(miscSection)
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
        AccountSettings.actionSettings(this, account)
    }


    companion object {
        @JvmStatic fun launch(activity: Activity) {
            val intent = Intent(activity, SettingsActivity::class.java)
            activity.startActivity(intent)
        }
    }
}
