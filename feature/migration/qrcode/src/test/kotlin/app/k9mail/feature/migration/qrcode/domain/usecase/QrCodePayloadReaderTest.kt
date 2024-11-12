package app.k9mail.feature.migration.qrcode.domain.usecase

import app.k9mail.core.common.mail.toUserEmailAddress
import app.k9mail.core.common.net.toHostname
import app.k9mail.core.common.net.toPort
import app.k9mail.feature.migration.qrcode.domain.entity.AccountData
import app.k9mail.feature.migration.qrcode.payload.QrCodePayloadAdapter
import app.k9mail.feature.migration.qrcode.payload.QrCodePayloadMapper
import app.k9mail.feature.migration.qrcode.payload.QrCodePayloadParser
import app.k9mail.feature.migration.qrcode.payload.QrCodePayloadValidator
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import kotlin.test.Test

@Suppress("LongMethod")
class QrCodePayloadReaderTest {
    private val reader = QrCodePayloadReader(
        parser = QrCodePayloadParser(QrCodePayloadAdapter()),
        mapper = QrCodePayloadMapper(QrCodePayloadValidator()),
    )

    @Test
    fun `one account, one identity, no passwords`() {
        val payload = """[1,[1,1],""" +
            """[0,"imap.domain.example",993,3,2,"username","My Account"],""" +
            """[[[0,"smtp.domain.example",587,2,1,"username"],""" +
            """["user@domain.example","Firstname Lastname"]]]]"""

        val result = reader.read(payload)

        assertThat(result).isNotNull().isEqualTo(
            AccountData(
                sequenceNumber = 1,
                sequenceEnd = 1,
                accounts = listOf(
                    AccountData.Account(
                        accountName = "My Account",
                        incomingServer = AccountData.IncomingServer(
                            protocol = AccountData.IncomingServerProtocol.Imap,
                            hostname = "imap.domain.example".toHostname(),
                            port = 993.toPort(),
                            connectionSecurity = AccountData.ConnectionSecurity.Tls,
                            authenticationType = AccountData.AuthenticationType.PasswordEncrypted,
                            username = "username",
                            password = null,
                        ),
                        outgoingServerGroups = listOf(
                            AccountData.OutgoingServerGroup(
                                outgoingServer = AccountData.OutgoingServer(
                                    protocol = AccountData.OutgoingServerProtocol.Smtp,
                                    hostname = "smtp.domain.example".toHostname(),
                                    port = 587.toPort(),
                                    connectionSecurity = AccountData.ConnectionSecurity.AlwaysStartTls,
                                    authenticationType = AccountData.AuthenticationType.PasswordCleartext,
                                    username = "username",
                                    password = null,
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
            ),
        )
    }

    @Test
    fun `two accounts, two identities each`() {
        val payload = """[1,[2,3],""" +
            """[0,"imap.domain.example",993,3,2,"username","","imap-password"],""" +
            """[[[0,"smtp.domain.example",587,2,1,"username","smtp-password"],""" +
            """["user@domain.example","Firstname Lastname"],""" +
            """["alias@domain.example","Nickname"]]],""" +
            """[0,"imap.company.example",143,2,1,"user@company.example","","company-password"],""" +
            """[[[0,"smtp.company.example",465,3,1,"user@company.example","company-password"],""" +
            """["user@company.example","Firstname Lastname"],""" +
            """["alias@company.example","Nickname Lastname"]]]]"""

        val result = reader.read(payload)

        assertThat(result).isNotNull().isEqualTo(
            AccountData(
                sequenceNumber = 2,
                sequenceEnd = 3,
                accounts = listOf(
                    AccountData.Account(
                        accountName = "user@domain.example",
                        incomingServer = AccountData.IncomingServer(
                            protocol = AccountData.IncomingServerProtocol.Imap,
                            hostname = "imap.domain.example".toHostname(),
                            port = 993.toPort(),
                            connectionSecurity = AccountData.ConnectionSecurity.Tls,
                            authenticationType = AccountData.AuthenticationType.PasswordEncrypted,
                            username = "username",
                            password = "imap-password",
                        ),
                        outgoingServerGroups = listOf(
                            AccountData.OutgoingServerGroup(
                                outgoingServer = AccountData.OutgoingServer(
                                    protocol = AccountData.OutgoingServerProtocol.Smtp,
                                    hostname = "smtp.domain.example".toHostname(),
                                    port = 587.toPort(),
                                    connectionSecurity = AccountData.ConnectionSecurity.AlwaysStartTls,
                                    authenticationType = AccountData.AuthenticationType.PasswordCleartext,
                                    username = "username",
                                    password = "smtp-password",
                                ),
                                identities = listOf(
                                    AccountData.Identity(
                                        emailAddress = "user@domain.example".toUserEmailAddress(),
                                        displayName = "Firstname Lastname",
                                    ),
                                    AccountData.Identity(
                                        emailAddress = "alias@domain.example".toUserEmailAddress(),
                                        displayName = "Nickname",
                                    ),
                                ),
                            ),
                        ),
                    ),
                    AccountData.Account(
                        accountName = "user@company.example",
                        incomingServer = AccountData.IncomingServer(
                            protocol = AccountData.IncomingServerProtocol.Imap,
                            hostname = "imap.company.example".toHostname(),
                            port = 143.toPort(),
                            connectionSecurity = AccountData.ConnectionSecurity.AlwaysStartTls,
                            authenticationType = AccountData.AuthenticationType.PasswordCleartext,
                            username = "user@company.example",
                            password = "company-password",
                        ),
                        outgoingServerGroups = listOf(
                            AccountData.OutgoingServerGroup(
                                outgoingServer = AccountData.OutgoingServer(
                                    protocol = AccountData.OutgoingServerProtocol.Smtp,
                                    hostname = "smtp.company.example".toHostname(),
                                    port = 465.toPort(),
                                    connectionSecurity = AccountData.ConnectionSecurity.Tls,
                                    authenticationType = AccountData.AuthenticationType.PasswordCleartext,
                                    username = "user@company.example",
                                    password = "company-password",
                                ),
                                identities = listOf(
                                    AccountData.Identity(
                                        emailAddress = "user@company.example".toUserEmailAddress(),
                                        displayName = "Firstname Lastname",
                                    ),
                                    AccountData.Identity(
                                        emailAddress = "alias@company.example".toUserEmailAddress(),
                                        displayName = "Nickname Lastname",
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `invalid payload`() {
        val payload = "https://domain.example/path"

        val result = reader.read(payload)

        assertThat(result).isNull()
    }
}
