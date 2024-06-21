package com.fsck.k9.activity

import android.os.AsyncTask
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import com.fsck.k9.Account
import com.fsck.k9.BaseAccount
import com.fsck.k9.K9.isShowUnifiedInbox
import com.fsck.k9.Preferences.Companion.getPreferences
import com.fsck.k9.search.SearchAccount.Companion.createUnifiedInboxAccount
import com.fsck.k9.ui.R
import com.google.android.material.textview.MaterialTextView

/**
 * Activity displaying the list of accounts.
 *
 *
 *
 * Classes extending this abstract class have to provide an [.onAccountSelected]
 * method to perform an action when an account is selected.
 *
 */
abstract class AccountList : K9ListActivity(), OnItemClickListener {
    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        setResult(RESULT_CANCELED)

        setLayout(R.layout.account_list)

        val listView = listView
        listView.onItemClickListener = this
        listView.itemsCanFocus = false
    }

    /**
     * Reload list of accounts when this activity is resumed.
     */
    public override fun onResume() {
        super.onResume()
        LoadAccounts().execute()
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        val account = parent.getItemAtPosition(position) as BaseAccount
        onAccountSelected(account)
    }

    /**
     * Create a new [AccountsAdapter] instance and assign it to the [ListView].
     *
     * @param realAccounts
     * An array of accounts to display.
     */
    fun populateListView(realAccounts: List<Account>?) {
        val accounts: MutableList<BaseAccount> = ArrayList()

        if (isShowUnifiedInbox) {
            val unifiedInboxAccount: BaseAccount = createUnifiedInboxAccount()
            accounts.add(unifiedInboxAccount)
        }

        accounts.addAll(realAccounts!!)
        val adapter: AccountsAdapter = AccountsAdapter(accounts)
        val listView = listView
        listView.adapter = adapter
        listView.invalidate()
    }

    /**
     * This method will be called when an account was selected.
     *
     * @param account
     * The account the user selected.
     */
    protected abstract fun onAccountSelected(account: BaseAccount?)

    internal inner class AccountsAdapter(accounts: List<BaseAccount?>?) : ArrayAdapter<BaseAccount?>(
        this@AccountList, 0, accounts!!,
    ) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val account = getItem(position)
            val view = convertView ?: layoutInflater.inflate(R.layout.accounts_item, parent, false)

            var holder: AccountViewHolder? = view.tag as AccountViewHolder
            if (holder == null) {
                holder = AccountViewHolder()
                holder!!.description = view.findViewById<MaterialTextView>(R.id.description)
                holder.email = view.findViewById<MaterialTextView>(R.id.email)
                holder.chip = view.findViewById<View>(R.id.chip)

                view.tag = holder
            }

            val accountName = account!!.name
            if (accountName != null) {
                holder!!.description!!.text = accountName
                holder.email!!.text = account.email
                holder.email!!.visibility = View.VISIBLE
            } else {
                holder!!.description!!.text = account.email
                holder.email!!.visibility = View.GONE
            }

            if (account is Account) {
                holder.chip!!.setBackgroundColor(account.chipColor)
            } else {
                holder.chip!!.setBackgroundColor(0xff999999.toInt())
            }

            holder.chip!!.background.alpha = 255


            return view
        }

        internal inner class AccountViewHolder {
            var description: MaterialTextView? = null
            var email: MaterialTextView? = null
            var chip: View? = null
        }
    }

    /**
     * Load accounts in a background thread
     */
    internal inner class LoadAccounts : AsyncTask<Void?, Void?, List<Account>>() {
        override fun doInBackground(vararg params: Void?): List<Account> {
            return getPreferences().getAccounts()
        }

        override fun onPostExecute(accounts: List<Account>) {
            populateListView(accounts)
        }
    }
}
