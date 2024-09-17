package app.k9mail.feature.migration.qrcode

import app.k9mail.core.common.mail.EmailAddressParserException
import app.k9mail.core.common.mail.toUserEmailAddress
import app.k9mail.core.common.net.toHostname
import app.k9mail.core.common.net.toPort
import timber.log.Timber

@Suppress("TooManyFunctions")
internal class QrCodePayloadValidator {
    fun isValid(data: QrCodeData): Boolean {
        if (data.version != 1) {
            Timber.d("Unsupported version: %s", data.version)
            return false
        }

        return try {
            validateData(data)
            true
        } catch (e: IllegalArgumentException) {
            Timber.d(e, "QR code payload failed validation")
            false
        }
    }

    private fun validateData(data: QrCodeData) {
        require(data.accounts.isNotEmpty()) { "Account array must not be empty" }

        for (account in data.accounts) {
            validateAccount(account)
        }
    }

    private fun validateAccount(account: QrCodeData.Account) {
        validateIncomingServer(account.incomingServer)
        validateOutgoingServers(account.outgoingServers)
    }

    private fun validateIncomingServer(incomingServer: QrCodeData.IncomingServer) {
        validateIncomingServerProtocol(incomingServer.protocol)
        validateHostname(incomingServer.hostname)
        validatePort(incomingServer.port)
        validateConnectionSecurity(incomingServer.connectionSecurity)
        validateAuthenticationType(incomingServer.authenticationType)
        validateUsername(incomingServer.username)
        validateAccountName(incomingServer.accountName)
        validatePassword(incomingServer.password)
    }

    private fun validateOutgoingServers(outgoingServers: List<QrCodeData.OutgoingServer>) {
        require(outgoingServers.isNotEmpty()) { "List of outgoing servers must not be empty" }

        for (outgoingServer in outgoingServers) {
            validateOutgoingServer(outgoingServer)
        }
    }

    private fun validateOutgoingServer(outgoingServer: QrCodeData.OutgoingServer) {
        validateOutgoingServerProtocol(outgoingServer.protocol)
        validateHostname(outgoingServer.hostname)
        validatePort(outgoingServer.port)
        validateConnectionSecurity(outgoingServer.connectionSecurity)
        validateAuthenticationType(outgoingServer.authenticationType)
        validateUsername(outgoingServer.username)
        validatePassword(outgoingServer.password)

        validateIdentities(outgoingServer.identities)
    }

    private fun validateIdentities(identities: List<QrCodeData.Identity>) {
        require(identities.isNotEmpty()) { "List of identities must not be empty" }

        for (identity in identities) {
            validateIdentity(identity)
        }
    }

    private fun validateIdentity(identity: QrCodeData.Identity) {
        validateEmailAddress(identity.emailAddress)
        validateDisplayName(identity.displayName)
    }

    private fun validateAccountName(accountName: String?) {
        require(accountName == null || isSingleLine(accountName)) { "Account name must not contain line break" }
    }

    private fun validateIncomingServerProtocol(protocol: Int) {
        AccountData.IncomingServerProtocol.fromInt(protocol)
    }

    private fun validateOutgoingServerProtocol(protocol: Int) {
        AccountData.OutgoingServerProtocol.fromInt(protocol)
    }

    private fun validateHostname(hostname: String) {
        hostname.toHostname()
    }

    private fun validatePort(port: Int) {
        port.toPort()
    }

    private fun validateConnectionSecurity(value: Int) {
        AccountData.ConnectionSecurity.fromInt(value)
    }

    private fun validateAuthenticationType(value: Int) {
        AccountData.AuthenticationType.fromInt(value)
    }

    private fun validateUsername(username: String) {
        require(isSingleLine(username)) { "Username must not contain line break" }
    }

    private fun validatePassword(password: String?) {
        require(password == null || isSingleLine(password)) { "Password must not contain line break" }
    }

    private fun validateEmailAddress(emailAddress: String) {
        try {
            emailAddress.toUserEmailAddress()
        } catch (e: EmailAddressParserException) {
            throw IllegalArgumentException("Email address failed to parse", e)
        }
    }

    private fun validateDisplayName(displayName: String) {
        require(isSingleLine(displayName)) { "Display name must not contain a line break" }
    }

    private fun isSingleLine(text: String): Boolean {
        return !text.contains(LINE_BREAK)
    }

    companion object {
        private val LINE_BREAK = "[\\r\\n]".toRegex()
    }
}
