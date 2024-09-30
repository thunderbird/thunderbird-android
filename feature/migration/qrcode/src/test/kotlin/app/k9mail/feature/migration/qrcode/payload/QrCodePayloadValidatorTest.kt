package app.k9mail.feature.migration.qrcode.payload

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test

class QrCodePayloadValidatorTest {
    private val validator = QrCodePayloadValidator()

    @Test
    fun `valid input`() {
        val input = INPUT

        val result = validator.isValid(input)

        assertThat(result).isTrue()
    }

    @Test
    fun `invalid account name`() {
        val input = INPUT.updateIncomingServer { server ->
            server.copy(accountName = "contains\nline break")
        }

        val result = validator.isValid(input)

        assertThat(result).isFalse()
    }

    @Test
    fun `incoming server with missing password`() {
        val input = INPUT.updateIncomingServer { server ->
            server.copy(password = null)
        }

        val result = validator.isValid(input)

        assertThat(result).isTrue()
    }

    @Test
    fun `outgoing server with missing password`() {
        val input = INPUT.updateOutgoingServer { server ->
            server.copy(password = null)
        }

        val result = validator.isValid(input)

        assertThat(result).isTrue()
    }

    @Test
    fun `unsupported version number`() {
        val input = INPUT.copy(version = 2)

        val result = validator.isValid(input)

        assertThat(result).isFalse()
    }

    @Test
    fun `empty list of accounts`() {
        val input = INPUT.copy(accounts = emptyList())

        val result = validator.isValid(input)

        assertThat(result).isFalse()
    }

    @Test
    fun `empty list of outgoing servers`() {
        val input = INPUT.copy(
            accounts = INPUT.accounts.map { it.copy(outgoingServers = emptyList()) },
        )

        val result = validator.isValid(input)

        assertThat(result).isFalse()
    }

    @Test
    fun `empty list of identities`() {
        val input = INPUT.copy(
            accounts = INPUT.accounts.map { account ->
                account.copy(
                    outgoingServers = account.outgoingServers.map { outgoingServer ->
                        outgoingServer.copy(identities = emptyList())
                    },
                )
            },
        )

        val result = validator.isValid(input)

        assertThat(result).isFalse()
    }

    @Test
    fun `invalid incoming server protocol`() {
        val input = INPUT.updateIncomingServer { server ->
            server.copy(protocol = 2)
        }

        val result = validator.isValid(input)

        assertThat(result).isFalse()
    }

    @Test
    fun `invalid incoming server hostname`() {
        val input = INPUT.updateIncomingServer { server ->
            server.copy(hostname = "invalid hostname")
        }

        val result = validator.isValid(input)

        assertThat(result).isFalse()
    }

    @Test
    fun `invalid incoming server port`() {
        val input = INPUT.updateIncomingServer { server ->
            server.copy(port = 100_000)
        }

        val result = validator.isValid(input)

        assertThat(result).isFalse()
    }

    @Test
    fun `invalid incoming server connection security`() {
        val input = INPUT.updateIncomingServer { server ->
            server.copy(connectionSecurity = 100)
        }

        val result = validator.isValid(input)

        assertThat(result).isFalse()
    }

    @Test
    fun `invalid incoming server authentication type`() {
        val input = INPUT.updateIncomingServer { server ->
            server.copy(authenticationType = 100)
        }

        val result = validator.isValid(input)

        assertThat(result).isFalse()
    }

    @Test
    fun `unsupported incoming server authentication type Gssapi`() {
        val input = INPUT.updateIncomingServer { server ->
            server.copy(authenticationType = 3)
        }

        val result = validator.isValid(input)

        assertThat(result).isFalse()
    }

    @Test
    fun `unsupported incoming server authentication type Ntlm`() {
        val input = INPUT.updateIncomingServer { server ->
            server.copy(authenticationType = 4)
        }

        val result = validator.isValid(input)

        assertThat(result).isFalse()
    }

    @Test
    fun `invalid incoming server username`() {
        val input = INPUT.updateIncomingServer { server ->
            server.copy(username = "contains\nline break")
        }

        val result = validator.isValid(input)

        assertThat(result).isFalse()
    }

    @Test
    fun `invalid incoming server password`() {
        val input = INPUT.updateIncomingServer { server ->
            server.copy(password = "contains\nline break")
        }

        val result = validator.isValid(input)

        assertThat(result).isFalse()
    }

    @Test
    fun `invalid outgoing server protocol`() {
        val input = INPUT.updateOutgoingServer { server ->
            server.copy(protocol = 1)
        }

        val result = validator.isValid(input)

        assertThat(result).isFalse()
    }

    @Test
    fun `invalid outgoing server hostname`() {
        val input = INPUT.updateOutgoingServer { server ->
            server.copy(hostname = "invalid hostname")
        }

        val result = validator.isValid(input)

        assertThat(result).isFalse()
    }

    @Test
    fun `invalid outgoing server port`() {
        val input = INPUT.updateOutgoingServer { server ->
            server.copy(port = 100_000)
        }

        val result = validator.isValid(input)

        assertThat(result).isFalse()
    }

    @Test
    fun `invalid outgoing server connection security`() {
        val input = INPUT.updateOutgoingServer { server ->
            server.copy(connectionSecurity = 100)
        }

        val result = validator.isValid(input)

        assertThat(result).isFalse()
    }

    @Test
    fun `invalid outgoing server authentication type`() {
        val input = INPUT.updateOutgoingServer { server ->
            server.copy(authenticationType = 100)
        }

        val result = validator.isValid(input)

        assertThat(result).isFalse()
    }

    @Test
    fun `unsupported outgoing server authentication type Gssapi`() {
        val input = INPUT.updateOutgoingServer { server ->
            server.copy(authenticationType = 3)
        }

        val result = validator.isValid(input)

        assertThat(result).isFalse()
    }

    @Test
    fun `unsupported outgoing server authentication type Ntlm`() {
        val input = INPUT.updateOutgoingServer { server ->
            server.copy(authenticationType = 4)
        }

        val result = validator.isValid(input)

        assertThat(result).isFalse()
    }

    @Test
    fun `invalid outgoing server username`() {
        val input = INPUT.updateOutgoingServer { server ->
            server.copy(username = "contains\nline break")
        }

        val result = validator.isValid(input)

        assertThat(result).isFalse()
    }

    @Test
    fun `invalid outgoing server password`() {
        val input = INPUT.updateOutgoingServer { server ->
            server.copy(password = "contains\nline break")
        }

        val result = validator.isValid(input)

        assertThat(result).isFalse()
    }

    @Test
    fun `invalid identity email address`() {
        val input = INPUT.updateIdentity { identity ->
            identity.copy(emailAddress = "invalid")
        }

        val result = validator.isValid(input)

        assertThat(result).isFalse()
    }

    @Test
    fun `invalid identity display name`() {
        val input = INPUT.updateIdentity { identity ->
            identity.copy(displayName = "contains\nline break")
        }

        val result = validator.isValid(input)

        assertThat(result).isFalse()
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

    private fun QrCodeData.updateOutgoingServer(
        block: (QrCodeData.OutgoingServer) -> QrCodeData.OutgoingServer,
    ): QrCodeData {
        return copy(
            accounts = accounts.map { account ->
                account.copy(
                    outgoingServers = account.outgoingServers.map(block),
                )
            },
        )
    }

    private fun QrCodeData.updateIdentity(
        block: (QrCodeData.Identity) -> QrCodeData.Identity,
    ): QrCodeData {
        return copy(
            accounts = accounts.map { account ->
                account.copy(
                    outgoingServers = account.outgoingServers.map { server ->
                        server.copy(
                            identities = server.identities.map(block),
                        )
                    },
                )
            },
        )
    }
}
