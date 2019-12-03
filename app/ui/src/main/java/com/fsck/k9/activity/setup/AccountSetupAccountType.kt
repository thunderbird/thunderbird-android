package com.fsck.k9.activity.setup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import android.widget.Toast
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.activity.K9Activity
import com.fsck.k9.helper.EmailHelper.getDomainFromEmailAddress
import com.fsck.k9.preferences.Protocols
import com.fsck.k9.setup.ServerNameSuggester
import com.fsck.k9.ui.R
import timber.log.Timber
import java.net.URI
import java.net.URISyntaxException

/**
 * Prompts the user to select an account type. The account type, along with the
 * passed in email address, password and makeDefault are then passed on to the
 * AccountSetupIncoming activity.
 */
class AccountSetupAccountType : K9Activity(), OnClickListener {
    private val serverNameSuggester = ServerNameSuggester()
    private var account: Account? = null
    private var makeDefault = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLayout(R.layout.account_setup_account_type)
        findViewById<View>(R.id.pop).setOnClickListener(this)
        findViewById<View>(R.id.imap).setOnClickListener(this)
        val accountUuid = intent.getStringExtra(EXTRA_ACCOUNT)
        account = Preferences.getPreferences(this).getAccount(accountUuid)
        makeDefault = intent.getBooleanExtra(EXTRA_MAKE_DEFAULT, false)
    }

    @Throws(URISyntaxException::class)
    private fun setupStoreAndSmtpTransport(serverType: String, schemePrefix: String) {
        val domainPart = getDomainFromEmailAddress(account!!.email)
        val suggestedStoreServerName = serverNameSuggester.suggestServerName(serverType, domainPart!!)
        val storeUriForDecode = URI(account!!.storeUri)
        val storeUri = URI(
            schemePrefix, storeUriForDecode.userInfo, suggestedStoreServerName,
            storeUriForDecode.port, null, null, null
        )
        account!!.storeUri = storeUri.toString()
        val suggestedTransportServerName = serverNameSuggester.suggestServerName(Protocols.SMTP, domainPart)
        val transportUriForDecode = URI(account!!.transportUri)
        val transportUri = URI(
            "smtp+tls+", transportUriForDecode.userInfo, suggestedTransportServerName,
            transportUriForDecode.port, null, null, null
        )
        account!!.transportUri = transportUri.toString()
    }

    override fun onClick(v: View) {
        try {
            val id = v.id
            if (id == R.id.pop) {
                setupStoreAndSmtpTransport(Protocols.POP3, "pop3+ssl+")
            } else if (id == R.id.imap) {
                setupStoreAndSmtpTransport(Protocols.IMAP, "imap+ssl+")
            }
        } catch (ex: Exception) {
            failure(ex)
        }
        AccountSetupIncoming.actionIncomingSettings(this, account, makeDefault)
        finish()
    }

    private fun failure(use: Exception) {
        Timber.e(use, "Failure")
        val toastText = getString(R.string.account_setup_bad_uri, use.message)
        val toast = Toast.makeText(application, toastText, Toast.LENGTH_LONG)
        toast.show()
    }

    companion object {
        private const val EXTRA_ACCOUNT = "account"
        private const val EXTRA_MAKE_DEFAULT = "makeDefault"

        @JvmStatic
        fun actionSelectAccountType(context: Context, account: Account, makeDefault: Boolean) {
            val i = Intent(context, AccountSetupAccountType::class.java)
            i.putExtra(EXTRA_ACCOUNT, account.uuid)
            i.putExtra(EXTRA_MAKE_DEFAULT, makeDefault)
            context.startActivity(i)
        }
    }
}
