package com.fsck.k9.ui.settings.account

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v7.preference.PreferenceFragmentCompat
import android.support.v7.preference.PreferenceFragmentCompat.OnPreferenceStartScreenCallback
import android.support.v7.preference.PreferenceScreen
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.activity.K9Activity
import com.fsck.k9.ui.R
import com.fsck.k9.ui.fragmentTransaction
import com.fsck.k9.ui.fragmentTransactionWithBackStack
import com.fsck.k9.ui.observe
import com.fsck.k9.ui.observeNotNull
import com.fsck.k9.ui.settings.SettingsViewModel
import kotlinx.android.synthetic.main.account_list_item.view.*
import kotlinx.android.synthetic.main.activity_account_settings.*
import kotlinx.android.synthetic.main.toolbar.*
import org.koin.android.architecture.ext.viewModel
import timber.log.Timber

class AccountSettingsActivity : K9Activity(), OnPreferenceStartScreenCallback, AdapterView.OnItemSelectedListener {
    private val accountViewModel: AccountSettingsViewModel by viewModel()
    private lateinit var accountUuid: String
    private var startScreenKey: String? = null
    private var fragmentAdded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLayout(R.layout.activity_account_settings)

        initializeActionBar()

        if (!decodeArguments()) {
            Timber.d("Invalid arguments")
            finish()
            return
        }

        loadAccount()
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        val uuid = accountSpinner.selection.uuid

        if (uuid == accountUuid)
            return;

        start(this, uuid)
        finish()
    }

    override fun onNothingSelected(parent: AdapterView<*>) {}

    private fun initializeActionBar() {
        val actionBar = supportActionBar ?: throw RuntimeException("getSupportActionBar() == null")
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setDisplayShowTitleEnabled(false)

        accountSpinner.title = title

        accountSpinner.onItemSelectedListener = this
        val prefs = Preferences.getPreferences(this)
        val viewModel: SettingsViewModel by viewModel()
        viewModel.accounts.observeNotNull(this) {
            accountSpinner.loadAccounts(prefs.accounts)
        }
    }

    private fun decodeArguments(): Boolean {
        accountUuid = intent.getStringExtra(ARG_ACCOUNT_UUID) ?: return false
        startScreenKey = intent.getStringExtra(ARG_START_SCREEN_KEY)
        return true
    }

    private fun loadAccount() {
        accountViewModel.getAccount(accountUuid).observe(this) { account ->
            if (account == null) {
                Timber.w("Account with UUID %s not found", accountUuid)
                finish()
                return@observe
            }

            accountSpinner.selection = account
            addAccountSettingsFragment()
        }
    }

    private fun addAccountSettingsFragment() {
        val needToAddFragment = supportFragmentManager.findFragmentById(R.id.accountSettingsContainer) == null
        if (needToAddFragment && !fragmentAdded) {
            fragmentAdded = true
            fragmentTransaction {
                add(R.id.accountSettingsContainer, AccountSettingsFragment.create(accountUuid, startScreenKey))
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onPreferenceStartScreen(
            caller: PreferenceFragmentCompat, preferenceScreen: PreferenceScreen
    ): Boolean {
        fragmentTransactionWithBackStack {
            replace(R.id.accountSettingsContainer, AccountSettingsFragment.create(accountUuid, preferenceScreen.key))
        }

        accountSpinner.title = preferenceScreen.title

        return true
    }

    override fun onBackPressed() {
        super.onBackPressed()

        accountSpinner.title = title
    }

    companion object {
        private const val ARG_ACCOUNT_UUID = "accountUuid"
        private const val ARG_START_SCREEN_KEY = "startScreen"

        @JvmStatic
        fun start(context: Context, accountUuid: String) {
            val intent = Intent(context, AccountSettingsActivity::class.java).apply {
                putExtra(ARG_ACCOUNT_UUID, accountUuid)
            }
            context.startActivity(intent)
        }

        @JvmStatic
        fun startCryptoSettings(context: Context, accountUuid: String) {
            val intent = Intent(context, AccountSettingsActivity::class.java).apply {
                putExtra(ARG_ACCOUNT_UUID, accountUuid)
                putExtra(ARG_START_SCREEN_KEY, AccountSettingsFragment.PREFERENCE_OPENPGP)
            }
            context.startActivity(intent)
        }
    }
}

class AccountSelectionSpinner : Spinner {
    var selection: Account
        get() = selectedItem as Account
        set(value) {
            val adapter = adapter as AccountsAdapter
            Spinner@setSelection(adapter.getPosition(value))
        }

    var title: CharSequence = ""
        set(value) {
            val adapter = adapter as AccountsAdapter
            adapter.title = value
            adapter.notifyDataSetChanged()
        }

    private lateinit var cachedBackground: Drawable

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    init {
        adapter = AccountsAdapter(context)
        cachedBackground = background
    }

    public fun loadAccounts(accounts: List<Account>) {
        val adapter = adapter as AccountsAdapter
        adapter.clear()
        adapter.addAll(accounts)

        setEnabled(accounts.size > 1)
        background = if (accounts.size > 1) cachedBackground else null
    }

    internal class AccountsAdapter(context: Context) : ArrayAdapter<Account>(context, 0)  {
        var title: CharSequence = ""

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val account = getItem(position)

            val view = convertView
                ?: LayoutInflater.from(context).inflate(R.layout.account_spinner_item, parent, false)

            return view.apply {
                setPadding(0, paddingTop, paddingRight, paddingBottom)
                name.text = AccountsAdapter@title
                email.text = account.email
            }
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View
        {
            val account = getItem(position)

            val view = convertView
                ?: LayoutInflater.from(context).inflate(R.layout.account_spinner_item, parent, false)

            return view.apply {
                setPadding(paddingLeft, paddingLeft / 2, paddingLeft, paddingLeft / 2)
                name.text = account.description
                email.text = account.email
            }
        }
    }
}
