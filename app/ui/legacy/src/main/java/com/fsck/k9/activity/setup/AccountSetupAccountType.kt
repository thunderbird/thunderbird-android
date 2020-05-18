package com.fsck.k9.activity.setup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.helper.EmailHelper.getDomainFromEmailAddress
import com.fsck.k9.mailstore.SpecialLocalFoldersCreator
import com.fsck.k9.preferences.Protocols
import com.fsck.k9.setup.ServerNameSuggester
import com.fsck.k9.ui.R
import com.fsck.k9.ui.base.K9Activity
import java.net.URI
import org.koin.android.ext.android.inject

/**
 * Prompts the user to select an account type. The account type, along with the
 * passed in email address, password and makeDefault are then passed on to the
 * AccountSetupIncoming activity.
 */
class AccountSetupAccountType : K9Activity() {
    private val preferences: Preferences by inject()
    private val serverNameSuggester: ServerNameSuggester by inject()
    private val localFoldersCreator: SpecialLocalFoldersCreator by inject()

    private lateinit var account: Account
    private var makeDefault = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLayout(R.layout.account_setup_account_type)

        decodeArguments()

        findViewById<View>(R.id.pop).setOnClickListener { setupPop3Account() }
        findViewById<View>(R.id.imap).setOnClickListener { setupImapAccount() }
    }

    private fun decodeArguments() {
        val accountUuid = intent.getStringExtra(EXTRA_ACCOUNT) ?: error("No account UUID provided")
        account = preferences.getAccount(accountUuid) ?: error("No account with given UUID found")
        makeDefault = intent.getBooleanExtra(EXTRA_MAKE_DEFAULT, false)
    }

    private fun setupPop3Account() {
        setupAccount(Protocols.POP3, "pop3+ssl+")
    }

    private fun setupImapAccount() {
        setupAccount(Protocols.IMAP, "imap+ssl+")
    }

    private fun setupAccount(serverType: String, schemePrefix: String) {
        setupStoreAndSmtpTransport(serverType, schemePrefix)
        createSpecialLocalFolders()
        returnAccountTypeSelectionResult()
    }

    private fun setupStoreAndSmtpTransport(serverType: String, schemePrefix: String) {
        val domainPart = getDomainFromEmailAddress(account.email) ?: error("Couldn't get domain from email address")

        setupStoreUri(serverType, domainPart, schemePrefix)
        setupTransportUri(domainPart)
    }

    private fun setupStoreUri(serverType: String, domainPart: String, schemePrefix: String) {
        val suggestedStoreServerName = serverNameSuggester.suggestServerName(serverType, domainPart)
        val storeUriForDecode = URI(account.storeUri)
        val storeUri = URI(
            schemePrefix, storeUriForDecode.userInfo, suggestedStoreServerName,
            storeUriForDecode.port, null, null, null
        )
        account.storeUri = storeUri.toString()
    }

    private fun setupTransportUri(domainPart: String) {
        val suggestedTransportServerName = serverNameSuggester.suggestServerName(Protocols.SMTP, domainPart)
        val transportUriForDecode = URI(account.transportUri)
        val transportUri = URI(
            "smtp+tls+", transportUriForDecode.userInfo, suggestedTransportServerName,
            transportUriForDecode.port, null, null, null
        )
        account.transportUri = transportUri.toString()
    }

    private fun createSpecialLocalFolders() {
        localFoldersCreator.createSpecialLocalFolders(account)
    }

    private fun returnAccountTypeSelectionResult() {
        AccountSetupIncoming.actionIncomingSettings(this, account, makeDefault)
        finish()
    }

    companion object {
        private const val EXTRA_ACCOUNT = "account"
        private const val EXTRA_MAKE_DEFAULT = "makeDefault"

        @JvmStatic
        fun actionSelectAccountType(context: Context, account: Account, makeDefault: Boolean) {
            val intent = Intent(context, AccountSetupAccountType::class.java).apply {
                putExtra(EXTRA_ACCOUNT, account.uuid)
                putExtra(EXTRA_MAKE_DEFAULT, makeDefault)
            }
            context.startActivity(intent)
        }
    }
}
