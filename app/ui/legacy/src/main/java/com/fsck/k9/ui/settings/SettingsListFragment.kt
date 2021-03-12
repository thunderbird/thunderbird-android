package com.fsck.k9.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.ui.R
import com.fsck.k9.ui.observeNotNull
import com.fsck.k9.ui.settings.account.AccountSettingsActivity
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.drag.ItemTouchCallback
import com.mikepenz.fastadapter.drag.SimpleDragCallback
import com.mikepenz.fastadapter.expandable.getExpandableExtension
import com.mikepenz.fastadapter.select.getSelectExtension
import com.mikepenz.fastadapter.utils.DragDropUtil
import kotlinx.android.synthetic.main.fragment_settings_list.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import androidx.appcompat.app.AppCompatDelegate

class SettingsListFragment : Fragment(), ItemTouchCallback {
    private val viewModel: SettingsViewModel by viewModel()

    private lateinit var settingsListAdapter: FastAdapter<GenericItem>
    private lateinit var itemAdapter: ItemAdapter<GenericItem>

    // drag & drop
    private lateinit var touchCallBack: SimpleDragCallback
    private lateinit var touchHelper: ItemTouchHelper

    private var numberOfAccounts = 0

    private lateinit var myVH : RecyclerView.ViewHolder // TODO This is a hack

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initializeSettingsList()
        populateSettingsList()
        // restore selections (this has to be done after the items were added
        settingsListAdapter.withSavedInstanceState(savedInstanceState)
    }

    private fun initializeSettingsList() {
        itemAdapter = ItemAdapter()

        settingsListAdapter = FastAdapter.with(itemAdapter).apply {
            setHasStableIds(true)
            onClickListener = { _, _, item, _ ->
                handleItemClick(item)
                true
            }
        }

        touchCallBack = SimpleDragCallback(this)
        touchHelper = ItemTouchHelper(touchCallBack)

        settingsListAdapter.getExpandableExtension()
        val selectExtension = settingsListAdapter.getSelectExtension()
        selectExtension.isSelectable = true
        touchCallBack.setIsDragEnabled(true)

        with(settings_list) {
            adapter = settingsListAdapter
            layoutManager = LinearLayoutManager(context)
            touchHelper.attachToRecyclerView(this)
        }
    }

    private fun populateSettingsList() {
        viewModel.accounts.observeNotNull(this) { accounts ->
            val accountsFinishedSetup = accounts.filter { it.isFinishedSetup }
            if (accountsFinishedSetup.isEmpty()) {
                launchOnboarding()
            } else {
                // populateSettingsList(accountsFinishedSetup)
                val listItems = buildSettingsList {
                    addAction(
                        text = getString(R.string.general_settings_title),
                        navigationAction = R.id.action_settingsListScreen_to_generalSettingsScreen,
                        icon = R.attr.iconSettingsGeneral
                    )

                    addSection(title = getString(R.string.accounts_title)) {
                        for (account in Preferences.getPreferences(context).accounts) {
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
                numberOfAccounts = accounts.size
            }
        }
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

    override fun itemTouchOnMove(oldPosition: Int, newPosition: Int): Boolean {
        val startDropPos = 2 // Accounts (the only draggable item) currently start as the third item in the List
        val endDropPos = startDropPos + numberOfAccounts
        // var accountItem = itemAdapter.getAdapterItem(newPosition) as AccountItem
        val preferences = Preferences.getPreferences(context)

        if (numberOfAccounts > 1 &&
            oldPosition >= startDropPos && oldPosition < endDropPos &&
            newPosition >= startDropPos && newPosition < endDropPos) {
            DragDropUtil.onMove(itemAdapter, oldPosition, newPosition)
            val moveUp = oldPosition <= newPosition
            val accountItem = itemAdapter.getAdapterItem(oldPosition) as AccountItem
            preferences.move(accountItem.account, moveUp)
            return true
        } else return false
    }

    override fun itemTouchDropped(oldPosition: Int, newPosition: Int) {
        myVH.itemView.setBackgroundColor(Color.TRANSPARENT)
    }

    override fun itemTouchStartDrag(viewHolder: RecyclerView.ViewHolder) {
        // add visual highlight to dragged item
        viewHolder.itemView.setBackgroundColor(Color.CYAN)
        myVH = viewHolder
    }
}