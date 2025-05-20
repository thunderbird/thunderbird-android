package com.fsck.k9.ui.settings.account

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.widget.AppCompatSpinner
import androidx.core.view.isVisible
import com.fsck.k9.ui.R
import com.google.android.material.textview.MaterialTextView
import net.thunderbird.core.android.account.LegacyAccount

class AccountSelectionSpinner : AppCompatSpinner {
    var selection: LegacyAccount
        get() = selectedItem as LegacyAccount
        set(account) {
            selectedAccount = account
            val adapter = adapter as AccountsAdapter
            val adapterPosition = adapter.getPosition(account)
            setSelection(adapterPosition, false)
        }

    private val cachedBackground: Drawable
    private var selectedAccount: LegacyAccount? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    init {
        adapter = AccountsAdapter(context)
        cachedBackground = background
    }

    fun setTitle(title: CharSequence) {
        val adapter = adapter as AccountsAdapter
        adapter.title = title
        adapter.notifyDataSetChanged()
    }

    fun setAccounts(accounts: List<LegacyAccount>) {
        val adapter = adapter as AccountsAdapter
        adapter.clear()
        adapter.addAll(accounts)

        selectedAccount?.let { selection = it }

        val showAccountSwitcher = accounts.size > 1
        isEnabled = showAccountSwitcher
        background = if (showAccountSwitcher) cachedBackground else null
    }

    internal class AccountsAdapter(context: Context) : ArrayAdapter<LegacyAccount>(context, 0) {
        var title: CharSequence = ""

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val account = getItem(position) ?: error("No item at position $position")

            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.account_spinner_item, parent, false)

            val name: MaterialTextView = view.findViewById(R.id.name)
            val email: MaterialTextView = view.findViewById(R.id.email)

            return view.apply {
                name.text = title
                email.text = account.displayName
            }
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            val account = getItem(position) ?: error("No item at position $position")

            val view = convertView
                ?: LayoutInflater.from(context).inflate(R.layout.account_spinner_dropdown_item, parent, false)

            val name: MaterialTextView = view.findViewById(R.id.name)
            val email: MaterialTextView = view.findViewById(R.id.email)

            return view.apply {
                val accountName = account.name
                if (accountName != null) {
                    name.text = accountName
                    email.text = account.email
                    email.isVisible = true
                } else {
                    name.text = account.email
                    email.isVisible = false
                }
            }
        }
    }
}
