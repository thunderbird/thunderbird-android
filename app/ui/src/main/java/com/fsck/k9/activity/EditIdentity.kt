package com.fsck.k9.activity

import android.os.Bundle
import android.view.View
import com.fsck.k9.Account
import com.fsck.k9.Identity
import com.fsck.k9.Preferences
import com.fsck.k9.ui.R
import kotlinx.android.synthetic.main.edit_identity.*

class EditIdentity : K9Activity() {
    private lateinit var mAccount: Account
    private lateinit var mIdentity: Identity
    private var mIdentityIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mIdentity = intent.getSerializableExtra(EXTRA_IDENTITY) as Identity
        mIdentityIndex = intent.getIntExtra(EXTRA_IDENTITY_INDEX, -1)
        val accountUuid = intent.getStringExtra(EXTRA_ACCOUNT)
        mAccount = Preferences.getPreferences(this).getAccount(accountUuid)

        if (mIdentityIndex == -1) {
            mIdentity = Identity()
        }

        setLayout(R.layout.edit_identity)

        /*
         * If we're being reloaded we override the original account with the one
         * we saved
         */
        if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_IDENTITY)) {
            mIdentity = savedInstanceState.getSerializable(EXTRA_IDENTITY) as Identity
        }

        description.setText(mIdentity.description)
        name.setText(mIdentity.name)
        email.setText(mIdentity.email)
        reply_to.setText(mIdentity.replyTo)

        //      mAccountAlwaysBcc = (EditText)findViewById(R.id.bcc);
        //      mAccountAlwaysBcc.setText(mIdentity.getAlwaysBcc());

        signature_use.isChecked = mIdentity.signatureUse
        signature_use.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                signature_layout.visibility = View.VISIBLE
                signature.setText(mIdentity.signature)
            } else {
                signature_layout.visibility = View.GONE
            }
        }

        if (signature_use.isChecked) {
            signature.setText(mIdentity.signature)
        } else {
            signature_layout.visibility = View.GONE
        }
    }

    private fun saveIdentity() {

        mIdentity.description = description.text.toString()
        mIdentity.email = email.text.toString()
        //      mIdentity.setAlwaysBcc(mAccountAlwaysBcc.getText().toString());
        mIdentity.name = name.text.toString()
        mIdentity.signatureUse = signature_use.isChecked
        mIdentity.signature = signature.text.toString()

        if (reply_to.text.isEmpty()) {
            mIdentity.replyTo = null
        } else {
            mIdentity.replyTo = reply_to.text.toString()
        }

        val identities = mAccount.identities
        if (mIdentityIndex == -1) {
            identities.add(mIdentity)
        } else {
            identities.removeAt(mIdentityIndex)
            identities.add(mIdentityIndex, mIdentity)
        }

        Preferences.getPreferences(applicationContext).saveAccount(mAccount)

        finish()
    }

    override fun onBackPressed() {
        saveIdentity()
        super.onBackPressed()
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(EXTRA_IDENTITY, mIdentity)
    }

    companion object {
        const val EXTRA_IDENTITY = "com.fsck.k9.EditIdentity_identity"
        const val EXTRA_IDENTITY_INDEX = "com.fsck.k9.EditIdentity_identity_index"
        const val EXTRA_ACCOUNT = "com.fsck.k9.EditIdentity_account"
    }
}
