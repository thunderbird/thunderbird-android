package com.fsck.k9.activity.setup

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.view.isVisible
import app.k9mail.legacy.account.Account
import com.fsck.k9.Preferences
import com.fsck.k9.ui.R
import com.fsck.k9.ui.base.K9Activity
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.radiobutton.MaterialRadioButton

class AccountSetupComposition : K9Activity() {
    private lateinit var account: Account

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
        setLayout(R.layout.account_setup_composition)
        setTitle(R.string.account_settings_composition_title)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val accountUuid = intent.getStringExtra(EXTRA_ACCOUNT) ?: error("Missing account UUID")
        account = Preferences.getPreferences().getAccount(accountUuid) ?: error("Couldn't find account")

        accountName = findViewById(R.id.account_name)
        accountEmail = findViewById(R.id.account_email)
        accountAlwaysBcc = findViewById(R.id.account_always_bcc)
        accountSignatureLayout = findViewById(R.id.account_signature_layout)
        accountSignatureUse = findViewById(R.id.account_signature_use)
        accountSignature = findViewById(R.id.account_signature)
        accountSignatureBeforeLocation = findViewById(R.id.account_signature_location_before_quoted_text)
        accountSignatureAfterLocation = findViewById(R.id.account_signature_location_after_quoted_text)

        accountName.setText(account.senderName)
        accountEmail.setText(account.email)
        accountAlwaysBcc.setText(account.alwaysBcc)

        val useSignature = account.signatureUse
        accountSignatureUse.isChecked = useSignature
        accountSignatureUse.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                accountSignatureLayout.isVisible = true
                accountSignature.setText(account.signature)

                val isSignatureBeforeQuotedText = account.isSignatureBeforeQuotedText
                accountSignatureBeforeLocation.isChecked = isSignatureBeforeQuotedText
                accountSignatureAfterLocation.isChecked = !isSignatureBeforeQuotedText
            } else {
                accountSignatureLayout.isVisible = false
            }
        }

        if (useSignature) {
            accountSignature.setText(account.signature)

            val isSignatureBeforeQuotedText = account.isSignatureBeforeQuotedText
            accountSignatureBeforeLocation.setChecked(isSignatureBeforeQuotedText)
            accountSignatureAfterLocation.setChecked(!isSignatureBeforeQuotedText)
        } else {
            accountSignatureLayout.isVisible = false
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
        account.email = accountEmail.text.toString()
        account.alwaysBcc = accountAlwaysBcc.text.toString()
        account.senderName = accountName.text.toString()
        account.signatureUse = accountSignatureUse.isChecked
        if (accountSignatureUse.isChecked) {
            account.signature = accountSignature.text.toString()
            account.isSignatureBeforeQuotedText = accountSignatureBeforeLocation.isChecked
        }

        Preferences.getPreferences().saveAccount(account)
    }

    public override fun onStop() {
        // TODO: Instead of saving the changes when the activity is stopped, add buttons to explicitly save or discard
        //  changes.
        saveSettings()
        super.onStop()
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
