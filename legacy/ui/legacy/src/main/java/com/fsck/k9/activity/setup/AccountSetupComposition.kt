package com.fsck.k9.activity.setup

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.fsck.k9.EmailAddressValidator
import com.fsck.k9.Preferences
import com.fsck.k9.ui.R
import com.fsck.k9.ui.base.BaseActivity
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.radiobutton.MaterialRadioButton
import net.thunderbird.core.android.account.LegacyAccountDto
import org.koin.android.ext.android.inject

class AccountSetupComposition : BaseActivity() {
    private val emailAddressValidator: EmailAddressValidator by inject()

    private lateinit var account: LegacyAccountDto

    private lateinit var accountSignature: EditText
    private lateinit var accountEmail: EditText
    private lateinit var accountAlwaysBcc: EditText
    private lateinit var accountSenderName: EditText
    private lateinit var accountSignatureUse: MaterialCheckBox
    private lateinit var accountSignatureBeforeLocation: MaterialRadioButton
    private lateinit var accountSignatureAfterLocation: MaterialRadioButton
    private lateinit var accountSignatureLayout: LinearLayout

    private var isSaveActionEnabled = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLayout(R.layout.account_setup_composition)
        setTitle(R.string.account_settings_composition_label)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val accountUuid = intent.getStringExtra(EXTRA_ACCOUNT) ?: error("Missing account UUID")
        account = Preferences.getPreferences().getAccount(accountUuid) ?: error("Couldn't find account")

        accountSenderName = findViewById(R.id.account_name)
        accountEmail = findViewById(R.id.account_email)
        accountAlwaysBcc = findViewById(R.id.account_always_bcc)
        accountSignatureLayout = findViewById(R.id.account_signature_layout)
        accountSignatureUse = findViewById(R.id.account_signature_use)
        accountSignature = findViewById(R.id.account_signature)
        accountSignatureBeforeLocation = findViewById(R.id.account_signature_location_before_quoted_text)
        accountSignatureAfterLocation = findViewById(R.id.account_signature_location_after_quoted_text)

        accountSenderName.setText(account.senderName)
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

        setTextChangedListeners()
        validateFields()
    }

    private fun setTextChangedListeners() {
        accountEmail.doAfterTextChanged { validateFields() }
    }

    private fun validateFields() {
        val valid = isValidEmailAddress(accountEmail)

        isSaveActionEnabled = valid
        invalidateOptionsMenu()
    }

    private fun isValidEmailAddress(textView: EditText): Boolean {
        return emailAddressValidator.isValidAddressOnly(textView.text.trim())
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.account_setup_composition_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.account_setup_composition_save).isEnabled = isSaveActionEnabled
        return true
    }

    @Suppress("ReturnCount")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        } else if (item.itemId == R.id.account_setup_composition_save) {
            saveSettings()
            finish()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun saveSettings() {
        account.email = accountEmail.text.toString().trim()
        account.alwaysBcc = accountAlwaysBcc.text.toString().takeUnless { it.isBlank() }
        account.senderName = accountSenderName.text.toString().takeUnless { it.isBlank() }
        account.signatureUse = accountSignatureUse.isChecked
        if (accountSignatureUse.isChecked) {
            account.signature = accountSignature.text.toString()
            account.isSignatureBeforeQuotedText = accountSignatureBeforeLocation.isChecked
        }

        Preferences.getPreferences().saveAccount(account)
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
