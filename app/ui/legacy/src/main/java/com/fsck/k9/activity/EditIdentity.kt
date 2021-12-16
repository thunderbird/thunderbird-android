package com.fsck.k9.activity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import androidx.core.view.isVisible
import com.fsck.k9.Account
import com.fsck.k9.Identity
import com.fsck.k9.Preferences
import com.fsck.k9.ui.R
import com.fsck.k9.ui.base.K9Activity

class EditIdentity : K9Activity() {
    private lateinit var account: Account
    private lateinit var identity: Identity

    private lateinit var description: TextView
    private lateinit var name: TextView
    private lateinit var email: TextView
    private lateinit var replyTo: TextView
    private lateinit var signatureUse: CheckBox
    private lateinit var signature: TextView
    private lateinit var signatureLayout: View

    private var identityIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLayout(R.layout.edit_identity)
        setTitle(R.string.edit_identity_title)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        identityIndex = intent.getIntExtra(EXTRA_IDENTITY_INDEX, -1)
        val accountUuid = intent.getStringExtra(EXTRA_ACCOUNT) ?: error("Missing account UUID")
        account = Preferences.getPreferences(this).getAccount(accountUuid) ?: error("Couldn't find account")

        identity = when {
            savedInstanceState != null -> savedInstanceState.getParcelable(EXTRA_IDENTITY) ?: error("Missing state")
            identityIndex != -1 -> intent.getParcelableExtra(EXTRA_IDENTITY) ?: error("Missing argument")
            else -> Identity()
        }

        description = findViewById(R.id.description)
        name = findViewById(R.id.name)
        email = findViewById(R.id.email)
        replyTo = findViewById(R.id.reply_to)
        signatureUse = findViewById(R.id.signature_use)
        signature = findViewById(R.id.signature)
        signatureLayout = findViewById(R.id.signature_layout)

        description.text = identity.description
        name.text = identity.name
        email.text = identity.email
        replyTo.text = identity.replyTo

        signatureUse.isChecked = identity.signatureUse
        signatureUse.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                signatureLayout.isVisible = true
                signature.text = identity.signature
            } else {
                signatureLayout.isVisible = false
            }
        }

        if (signatureUse.isChecked) {
            signature.text = identity.signature
        } else {
            signatureLayout.isVisible = false
        }
    }

    private fun saveIdentity() {
        identity = identity.copy(
            description = description.text.toString(),
            email = email.text.toString(),
            name = name.text.toString(),
            signatureUse = signatureUse.isChecked,
            signature = signature.text.toString(),
            replyTo = if (replyTo.text.isNotEmpty()) replyTo.text.toString() else null
        )

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

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(EXTRA_IDENTITY, identity)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.edit_identity_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        } else if (item.itemId == R.id.edit_identity_save) {
            saveIdentity()
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val EXTRA_IDENTITY = "com.fsck.k9.EditIdentity_identity"
        const val EXTRA_IDENTITY_INDEX = "com.fsck.k9.EditIdentity_identity_index"
        const val EXTRA_ACCOUNT = "com.fsck.k9.EditIdentity_account"
    }
}
