package com.fsck.k9.account

import android.content.res.Resources
import com.fsck.k9.Account.DeletePolicy
import com.fsck.k9.Preferences
import com.fsck.k9.core.R
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.preferences.Protocols

/**
 * Deals with logic surrounding account creation.
 *
 * TODO Move much of the code from com.fsck.k9.activity.setup.* into here
 */
class AccountCreator(private val preferences: Preferences, private val resources: Resources) {

    fun getDefaultDeletePolicy(type: String): DeletePolicy {
        return when (type) {
            Protocols.IMAP -> DeletePolicy.ON_DELETE
            Protocols.POP3 -> DeletePolicy.NEVER
            Protocols.WEBDAV -> DeletePolicy.ON_DELETE
            "demo" -> DeletePolicy.ON_DELETE
            else -> throw AssertionError("Unhandled case: $type")
        }
    }

    fun getDefaultPort(securityType: ConnectionSecurity, serverType: String): Int {
        return when (serverType) {
            Protocols.IMAP -> getImapDefaultPort(securityType)
            Protocols.WEBDAV -> getWebDavDefaultPort(securityType)
            Protocols.POP3 -> getPop3DefaultPort(securityType)
            Protocols.SMTP -> getSmtpDefaultPort(securityType)
            else -> throw AssertionError("Unhandled case: $serverType")
        }
    }

    private fun getImapDefaultPort(connectionSecurity: ConnectionSecurity): Int {
        return if (connectionSecurity == ConnectionSecurity.SSL_TLS_REQUIRED) 993 else 143
    }

    private fun getPop3DefaultPort(connectionSecurity: ConnectionSecurity): Int {
        return if (connectionSecurity == ConnectionSecurity.SSL_TLS_REQUIRED) 995 else 110
    }

    private fun getWebDavDefaultPort(connectionSecurity: ConnectionSecurity): Int {
        return if (connectionSecurity == ConnectionSecurity.SSL_TLS_REQUIRED) 443 else 80
    }

    private fun getSmtpDefaultPort(connectionSecurity: ConnectionSecurity): Int {
        return if (connectionSecurity == ConnectionSecurity.SSL_TLS_REQUIRED) 465 else 587
    }

    fun pickColor(): Int {
        val accounts = preferences.accounts
        val usedAccountColors = accounts.map { it.chipColor }.toSet()
        val accountColors = resources.getIntArray(R.array.account_colors).toList()

        val availableColors = accountColors - usedAccountColors
        if (availableColors.isEmpty()) {
            return accountColors.random()
        }

        val defaultAccountColors = resources.getIntArray(R.array.default_account_colors)
        return availableColors.shuffled().minByOrNull { color ->
            val index = defaultAccountColors.indexOf(color)
            if (index != -1) index else defaultAccountColors.size
        } ?: error("availableColors must not be empty")
    }
}
