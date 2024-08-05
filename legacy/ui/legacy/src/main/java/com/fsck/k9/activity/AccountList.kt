package com.fsck.k9.activity

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.k9mail.legacy.account.Account
import app.k9mail.legacy.account.BaseAccount
import com.fsck.k9.CoreResourceProvider
import com.fsck.k9.K9.isShowUnifiedInbox
import com.fsck.k9.Preferences.Companion.getPreferences
import com.fsck.k9.search.SearchAccount.Companion.createUnifiedInboxAccount
import com.fsck.k9.ui.R
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

/**
 * Activity displaying the list of accounts.
 *
 * Classes extending this abstract class have to provide an [.onAccountSelected]
 * method to perform an action when an account is selected.
 */
abstract class AccountList : K9ListActivity(), OnItemClickListener {

    private val coreResourceProvider: CoreResourceProvider by inject()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        setLayout(R.layout.account_list)

        listView.apply {
            onItemClickListener = this@AccountList
            itemsCanFocus = false
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                loadAccounts()
            }
        }
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        val account = parent.getItemAtPosition(position) as BaseAccount
        onAccountSelected(account)
    }

    /**
     * Load accounts in a background thread and populate the list view with the results on main thread.
     */
    private suspend fun loadAccounts() {
        val accounts = withContext(Dispatchers.IO) {
            getPreferences().getAccounts()
        }

        populateListView(accounts)
    }

    /**
     * Create a new [AccountsAdapter] instance and assign it to the [android.widget.ListView].
     *
     * @param realAccounts
     * An array of accounts to display.
     */
    private fun populateListView(realAccounts: List<Account>) {
        val accounts: MutableList<BaseAccount> = ArrayList()

        if (isShowUnifiedInbox) {
            val unifiedInboxAccount: BaseAccount = createUnifiedInboxAccount(
                unifiedInboxTitle = coreResourceProvider.searchUnifiedInboxTitle(),
                unifiedInboxDetail = coreResourceProvider.searchUnifiedInboxDetail(),
            )
            accounts.add(unifiedInboxAccount)
        }

        accounts.addAll(realAccounts)

        listView.apply {
            adapter = AccountsAdapter(accounts)
            invalidate()
        }
    }

    /**
     * This method will be called when an account was selected.
     *
     * @param account
     * The account the user selected.
     */
    protected abstract fun onAccountSelected(account: BaseAccount)

    internal inner class AccountsAdapter(accounts: List<BaseAccount?>) : ArrayAdapter<BaseAccount?>(
        this@AccountList,
        0,
        accounts,
    ) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val account = getItem(position)
            val view = convertView ?: layoutInflater.inflate(R.layout.accounts_item, parent, false)

            val holder = (view.tag as? AccountViewHolder) ?: AccountViewHolder(view).apply {
                view.tag = this
            }

            val accountName = account!!.name
            if (accountName != null) {
                holder.description.text = accountName
                holder.email.text = account.email
                holder.email.visibility = View.VISIBLE
            } else {
                holder.description.text = account.email
                holder.email.visibility = View.GONE
            }

            if (account is Account) {
                holder.chip.setBackgroundColor(account.chipColor)
            } else {
                holder.chip.setBackgroundColor(resources.getColor(R.color.account_list_item_chip_background))
            }

            holder.chip.background.alpha = BACKGROUND_ALPHA

            return view
        }

        internal inner class AccountViewHolder(view: View) {
            var description: MaterialTextView = view.findViewById(R.id.description)
            var email: MaterialTextView = view.findViewById(R.id.email)
            var chip: View = view.findViewById(R.id.chip)
        }
    }

    private companion object {
        const val BACKGROUND_ALPHA = 255
    }
}
