package com.fsck.k9.activity

import android.os.Bundle
import androidx.core.view.isVisible
import com.fsck.k9.Account
import com.fsck.k9.Identity
import com.fsck.k9.Preferences
import com.fsck.k9.ui.R
import com.fsck.k9.ui.base.K9Activity
import kotlinx.android.synthetic.main.edit_identity.*

class EditIdentity : K9Activity() {
    private lateinit var account: Account
    private lateinit var identity: Identity
    private var identityIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLayout(R.layout.edit_identity)

        identityIndex = intent.getIntExtra(EXTRA_IDENTITY_INDEX, -1)
        val accountUuid = intent.getStringExtra(EXTRA_ACCOUNT)
        account = Preferences.getPreferences(this).getAccount(accountUuid)

        identity = when {
            savedInstanceState != null -> savedInstanceState.getParcelable(EXTRA_IDENTITY) ?: error("Missing state")
            identityIndex != -1 -> intent.getParcelableExtra(EXTRA_IDENTITY) ?: error("Missing argument")
            else -> Identity()
        }

        description.setText(identity.description)
        name.setText(identity.name)
        email.setText(identity.email)
        reply_to.setText(identity.replyTo)

        //      mAccountAlwaysBcc = (EditText)findViewById(R.id.bcc);
        //      mAccountAlwaysBcc.setText(identity.getAlwaysBcc());

        signature_use.isChecked = identity.signatureUse
        signature_use.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                signature_layout.isVisible = true
                signature.setText(identity.signature)
            } else {
                signature_layout.isVisible = false
            }
        }

        if (signature_use.isChecked) {
            signature.setText(identity.signature)
        } else {
            signature_layout.isVisible = false
        }
    }

    private fun saveIdentity() {
        identity = identity.copy(
            description = description.text.toString(),
            email = email.text.toString(),
            name = name.text.toString(),
            signatureUse = signature_use.isChecked,
            signature = signature.text.toString(),
            replyTo = if (reply_to.text.isNotEmpty()) reply_to.text.toString() else null
        )
        // identity.setAlwaysBcc(mAccountAlwaysBcc.getText().toString());

        val identities = account.identities
        if (identityIndex == -1) {
            identities.add(identity)
        } else {
            identities.removeAt(identityIndex)
            identities.add(identityIndex, identity)
        }

        Preferences.getPreferences(applicationContext).saveAccount(account)

        finish()
    }

    override fun onBackPressed() {
        saveIdentity()
        super.onBackPressed()
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(EXTRA_IDENTITY, identity)
    }

    companion object {
        const val EXTRA_IDENTITY = "com.fsck.k9.EditIdentity_identity"
        const val EXTRA_IDENTITY_INDEX = "com.fsck.k9.EditIdentity_identity_index"
        const val EXTRA_ACCOUNT = "com.fsck.k9.EditIdentity_account"
    }
}
