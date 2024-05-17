package com.fsck.k9.ui.settings.account

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceFragmentCompat.OnPreferenceStartScreenCallback
import androidx.preference.PreferenceScreen
import com.fsck.k9.ui.R
import com.fsck.k9.ui.base.K9Activity
import com.fsck.k9.ui.base.livedata.observeNotNull
import com.fsck.k9.ui.fragmentTransaction
import com.fsck.k9.ui.fragmentTransactionWithBackStack
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class AccountSettingsActivity : K9Activity(), OnPreferenceStartScreenCallback {
    private val accountViewModel: AccountSettingsViewModel by viewModel()
    private lateinit var accountUuid: String
    private var startScreenKey: String? = null
    private var fragmentAdded = false

    private lateinit var accountSpinner: AccountSelectionSpinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLayout(R.layout.activity_account_settings)

        accountSpinner = findViewById(R.id.accountSpinner)

        initializeActionBar()

        if (!decodeArguments()) {
            Timber.d("Invalid arguments")
            finish()
            return
        }

        loadAccount()
    }

    private fun initializeActionBar() {
        val actionBar = supportActionBar ?: throw RuntimeException("getSupportActionBar() == null")
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setDisplayShowTitleEnabled(false)

        accountSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                onAccountSelected(selectedAccountUuid = accountSpinner.selection.uuid)
            }

            override fun onNothingSelected(parent: AdapterView<*>) = Unit
        }

        accountViewModel.accounts.observeNotNull(this) { accounts ->
            accountSpinner.setAccounts(accounts)
        }
    }

    private fun onAccountSelected(selectedAccountUuid: String) {
        if (selectedAccountUuid != accountUuid && !isFinishing) {
            start(this, selectedAccountUuid)
            finish()
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
            onBackPressedDispatcher.onBackPressed()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onPreferenceStartScreen(
        caller: PreferenceFragmentCompat,
        preferenceScreen: PreferenceScreen,
    ): Boolean {
        fragmentTransactionWithBackStack {
            replace(R.id.accountSettingsContainer, AccountSettingsFragment.create(accountUuid, preferenceScreen.key))
        }

        return true
    }

    override fun setTitle(title: CharSequence) {
        super.setTitle(title)
        accountSpinner.setTitle(title)
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
