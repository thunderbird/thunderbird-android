package app.k9mail.feature.migration.qrcode.payload

import app.k9mail.core.common.mail.toUserEmailAddress
import app.k9mail.core.common.net.toHostname
import app.k9mail.core.common.net.toPort
import assertk.assertThat
import assertk.assertions.first
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.prop
import kotlin.test.Test

class QrCodePayloadMapperTest {
    private val mapper = QrCodePayloadMapper()

    @Test
    fun `valid input should be mapped to expected output`() {
        val input = INPUT

        val result = mapper.toAccountData(input)

        assertThat(result).isEqualTo(OUTPUT)
    }

    @Test
    fun `use email address of first identity when account name is the empty string`() {
        val input = INPUT.updateIncomingServer { server ->
            server.copy(accountName = "")
        }

        val result = mapper.toAccountData(input)

        assertThat(result).isNotNull()
            .prop(AccountData::accounts).first()
            .prop(AccountData.Account::accountName).isEqualTo("user@domain.example")
    }

    @Test
    fun `use email address of first identity when account name is missing`() {
        val input = INPUT.updateIncomingServer { server ->
            server.copy(accountName = null, password = null)
        }

        val result = mapper.toAccountData(input)

        assertThat(result).isNotNull()
            .prop(AccountData::accounts).first()
            .prop(AccountData.Account::accountName).isEqualTo("user@domain.example")
    }

    companion object {
        private val INPUT = QrCodeData(
            version = 1,
            misc = QrCodeData.Misc(
                sequenceNumber = 1,
                sequenceEnd = 1,
            ),
            accounts = listOf(
                QrCodeData.Account(
                    incomingServer = QrCodeData.IncomingServer(
                        protocol = 0,
                        hostname = "imap.domain.example",
                        port = 993,
                        connectionSecurity = 3,
                        authenticationType = 1,
                        username = "user@domain.example",
                        accountName = "Account name",
                        password = "password",
                    ),
                    outgoingServers = listOf(
                        QrCodeData.OutgoingServer(
                            protocol = 0,
                            hostname = "smtp.domain.example",
                            port = 465,
                            connectionSecurity = 3,
                            authenticationType = 1,
                            username = "user@domain.example",
                            password = "password",
                            identities = listOf(
                                QrCodeData.Identity(
                                    emailAddress = "user@domain.example",
                                    displayName = "Firstname Lastname",
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        private val OUTPUT = AccountData(
            sequenceNumber = 1,
            sequenceEnd = 1,
            accounts = listOf(
                AccountData.Account(
                    accountName = "Account name",
                    incomingServer = AccountData.IncomingServer(
                        protocol = AccountData.IncomingServerProtocol.Imap,
                        hostname = "imap.domain.example".toHostname(),
                        port = 993.toPort(),
                        connectionSecurity = AccountData.ConnectionSecurity.Tls,
                        authenticationType = AccountData.AuthenticationType.PasswordCleartext,
                        username = "user@domain.example",
                        password = "password",
                    ),
                    outgoingServerGroups = listOf(
                        AccountData.OutgoingServerGroup(
                            outgoingServer = AccountData.OutgoingServer(
                                protocol = AccountData.OutgoingServerProtocol.Smtp,
                                hostname = "smtp.domain.example".toHostname(),
                                port = 465.toPort(),
                                connectionSecurity = AccountData.ConnectionSecurity.Tls,
                                authenticationType = AccountData.AuthenticationType.PasswordCleartext,
                                username = "user@domain.example",
                                password = "password",
                            ),
                            identities = listOf(
                                AccountData.Identity(
                                    emailAddress = "user@domain.example".toUserEmailAddress(),
                                    displayName = "Firstname Lastname",
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )
    }

    private fun QrCodeData.updateIncomingServer(
        block: (QrCodeData.IncomingServer) -> QrCodeData.IncomingServer,
    ): QrCodeData {
        return copy(
            accounts = accounts.map { account ->
                account.copy(
                    incomingServer = block(account.incomingServer),
                )
            },
        )
    }
}
