package com.fsck.k9.activity.setup

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.LinearLayout
import app.k9mail.legacy.account.Account
import com.fsck.k9.Preferences.Companion.getPreferences
import com.fsck.k9.ui.R
import com.fsck.k9.ui.base.K9Activity
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.radiobutton.MaterialRadioButton

class AccountSetupComposition : K9Activity() {
    private var account: Account? = null

    private lateinit var accountSignature: EditText
    private lateinit var accountEmail: EditText
    private lateinit var accountAlwaysBcc: EditText
    private lateinit var accountName: EditText
    private lateinit var accountSignatureUse: MaterialCheckBox
    private lateinit var accountSignatureBeforeLocation: MaterialRadioButton
    private lateinit var accountSignatureAfterLocation: MaterialRadioButton
    private lateinit var accountSignatureLayout: LinearLayout

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var accountUuid = intent.getStringExtra(EXTRA_ACCOUNT)
        account = getPreferences().getAccount(accountUuid!!)

        setLayout(R.layout.account_setup_composition)
        setTitle(R.string.account_settings_composition_title)

        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }

        /*
         * If we're being reloaded we override the original account with the one
         * we saved
         */
        if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_ACCOUNT)) {
            accountUuid = savedInstanceState.getString(EXTRA_ACCOUNT)
            account = getPreferences().getAccount(accountUuid!!)
        }

        accountName = findViewById(R.id.account_name)
        accountName.setText(account!!.senderName)

        accountEmail = findViewById(R.id.account_email)
        accountEmail.setText(account!!.email)

        accountAlwaysBcc = findViewById(R.id.account_always_bcc)
        accountAlwaysBcc.setText(account!!.alwaysBcc)

        accountSignatureLayout = findViewById(R.id.account_signature_layout)

        accountSignatureUse = findViewById(R.id.account_signature_use)
        val useSignature = account!!.signatureUse
        accountSignatureUse.setChecked(useSignature)
        accountSignatureUse.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                accountSignatureLayout.setVisibility(View.VISIBLE)
                accountSignature.setText(account!!.signature)
                val isSignatureBeforeQuotedText = account!!.isSignatureBeforeQuotedText
                accountSignatureBeforeLocation.isChecked = isSignatureBeforeQuotedText
                accountSignatureAfterLocation.isChecked = !isSignatureBeforeQuotedText
            } else {
                accountSignatureLayout.setVisibility(View.GONE)
            }
        })

        accountSignature = findViewById(R.id.account_signature)

        accountSignatureBeforeLocation = findViewById(R.id.account_signature_location_before_quoted_text)
        accountSignatureAfterLocation = findViewById(R.id.account_signature_location_after_quoted_text)

        if (useSignature) {
            accountSignature.setText(account!!.signature)

            val isSignatureBeforeQuotedText = account!!.isSignatureBeforeQuotedText
            accountSignatureBeforeLocation.setChecked(isSignatureBeforeQuotedText)
            accountSignatureAfterLocation.setChecked(!isSignatureBeforeQuotedText)
        } else {
            accountSignatureLayout.setVisibility(View.GONE)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun saveSettings() {
        account!!.email = accountEmail.text.toString()
        account!!.alwaysBcc = accountAlwaysBcc.text.toString()
        account!!.senderName = accountName.text.toString()
        account!!.signatureUse = accountSignatureUse.isChecked
        if (accountSignatureUse.isChecked) {
            account!!.signature = accountSignature.text.toString()
            val isSignatureBeforeQuotedText = accountSignatureBeforeLocation.isChecked
            account!!.isSignatureBeforeQuotedText = isSignatureBeforeQuotedText
        }

        getPreferences().saveAccount(account!!)
    }

    public override fun onStop() {
        // TODO: Instead of saving the changes when the activity is stopped, add buttons to explicitly save or discard
        //  changes.
        saveSettings()
        super.onStop()
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(EXTRA_ACCOUNT, account!!.uuid)
    }

    companion object {
        private const val EXTRA_ACCOUNT = "account"

        fun actionEditCompositionSettings(context: Activity, accountUuid: String?) {
            val intent = Intent(context, AccountSetupComposition::class.java)
            intent.setAction(Intent.ACTION_EDIT)
            intent.putExtra(EXTRA_ACCOUNT, accountUuid)
            context.startActivity(intent)
        }
    }
}
