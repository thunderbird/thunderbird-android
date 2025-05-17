package app.k9mail.feature.migration.qrcode.payload

import app.k9mail.core.common.mail.Protocols
import app.k9mail.core.common.mail.toUserEmailAddress
import app.k9mail.core.common.net.toHostname
import app.k9mail.core.common.net.toPort
import app.k9mail.feature.migration.qrcode.domain.entity.AccountData
import app.k9mail.feature.migration.qrcode.domain.entity.AccountData.IncomingServerProtocol
import com.fsck.k9.account.DeletePolicyProvider
import net.thunderbird.core.android.account.DeletePolicy

internal class QrCodePayloadMapper(
    private val qrCodePayloadValidator: QrCodePayloadValidator,
    private val deletePolicyProvider: DeletePolicyProvider,
) {
    fun toAccountData(data: QrCodeData): AccountData? {
        return if (qrCodePayloadValidator.isValid(data)) {
            mapToAccountData(data)
        } else {
            null
        }
    }

    private fun mapToAccountData(data: QrCodeData): AccountData {
        return AccountData(
            sequenceNumber = data.misc.sequenceNumber,
            sequenceEnd = data.misc.sequenceEnd,
            accounts = data.accounts.map { account -> mapAccount(account) },
        )
    }

    private fun mapAccount(account: QrCodeData.Account): AccountData.Account {
        val incomingServer = mapIncomingServer(account.incomingServer)
        val outgoingServerGroups = mapOutgoingServerGroups(account.outgoingServers)
        val accountName = mapAccountName(
            accountName = account.incomingServer.accountName,
            identity = outgoingServerGroups.first().identities.first(),
        )
        val deletePolicy = getDeletePolicy(incomingServer.protocol)

        return AccountData.Account(
            accountName = accountName,
            deletePolicy = deletePolicy,
            incomingServer = incomingServer,
            outgoingServerGroups = outgoingServerGroups,
        )
    }

    private fun mapAccountName(accountName: String?, identity: AccountData.Identity): String {
        // When setting up an account in Thunderbird, the account name matches the email address. We can avoid this
        // duplication in the encoded data by omitting the account name when it matches the email address.
        // This method will return the email address of the first identity in case the account name is null or the empty
        // string.
        return accountName?.takeIf { it.isNotEmpty() } ?: identity.emailAddress.toString()
    }

    private fun mapIncomingServer(incomingServer: QrCodeData.IncomingServer): AccountData.IncomingServer {
        return AccountData.IncomingServer(
            protocol = incomingServer.protocol.toIncomingServerProtocol(),
            hostname = incomingServer.hostname.toHostname(),
            port = incomingServer.port.toPort(),
            connectionSecurity = incomingServer.connectionSecurity.toConnectionSecurity(),
            authenticationType = incomingServer.authenticationType.toAuthenticationType(),
            username = incomingServer.username,
            password = incomingServer.password,
        )
    }

    private fun mapOutgoingServerGroups(
        outgoingServers: List<QrCodeData.OutgoingServer>,
    ): List<AccountData.OutgoingServerGroup> {
        return outgoingServers.map { outgoingServer ->
            AccountData.OutgoingServerGroup(
                outgoingServer = mapOutgoingServer(outgoingServer),
                identities = mapIdentities(outgoingServer.identities),
            )
        }
    }

    private fun mapOutgoingServer(outgoingServer: QrCodeData.OutgoingServer): AccountData.OutgoingServer {
        return AccountData.OutgoingServer(
            protocol = outgoingServer.protocol.toOutgoingServerProtocol(),
            hostname = outgoingServer.hostname.toHostname(),
            port = outgoingServer.port.toPort(),
            connectionSecurity = outgoingServer.connectionSecurity.toConnectionSecurity(),
            authenticationType = outgoingServer.authenticationType.toAuthenticationType(),
            username = outgoingServer.username,
            password = outgoingServer.password,
        )
    }

    private fun mapIdentities(identities: List<QrCodeData.Identity>): List<AccountData.Identity> {
        return identities.map { identity -> mapIdentity(identity) }
    }

    private fun mapIdentity(identity: QrCodeData.Identity): AccountData.Identity {
        return AccountData.Identity(
            emailAddress = identity.emailAddress.toUserEmailAddress(),
            displayName = identity.displayName,
        )
    }

    private fun getDeletePolicy(protocol: IncomingServerProtocol): DeletePolicy {
        val accountType = when (protocol) {
            IncomingServerProtocol.Imap -> Protocols.IMAP
            IncomingServerProtocol.Pop3 -> Protocols.POP3
        }

        return deletePolicyProvider.getDeletePolicy(accountType)
    }
}
