package app.k9mail.feature.migration.qrcode.payload

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import kotlin.test.Test

@Suppress("LongMethod")
class QrCodePayloadParserTest {
    private val parser = QrCodePayloadParser(QrCodePayloadAdapter())

    @Test
    fun `one account, one identity, no account name, no passwords`() {
        val payload = """[1,[1,1],""" +
            """[0,"imap.domain.example",993,3,2,"username"],""" +
            """[[[0,"smtp.domain.example",587,2,1,"username"],""" +
            """["user@domain.example","Firstname Lastname"]]]]"""

        val result = parser.parse(payload)

        assertThat(result).isNotNull().isEqualTo(
            QrCodeData(
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
                            authenticationType = 2,
                            username = "username",
                            accountName = null,
                            password = null,
                        ),
                        outgoingServers = listOf(
                            QrCodeData.OutgoingServer(
                                protocol = 0,
                                hostname = "smtp.domain.example",
                                port = 587,
                                connectionSecurity = 2,
                                authenticationType = 1,
                                username = "username",
                                password = null,
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
            ),
        )
    }

    @Test
    fun `one account, one identity, with account name, no passwords`() {
        val payload = """[1,[1,1],""" +
            """[0,"imap.domain.example",993,3,2,"username","Personal"],""" +
            """[[[0,"smtp.domain.example",587,2,1,"username"],""" +
            """["user@domain.example","Firstname Lastname"]]]]"""

        val result = parser.parse(payload)

        assertThat(result).isNotNull().isEqualTo(
            QrCodeData(
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
                            authenticationType = 2,
                            username = "username",
                            accountName = "Personal",
                            password = null,
                        ),
                        outgoingServers = listOf(
                            QrCodeData.OutgoingServer(
                                protocol = 0,
                                hostname = "smtp.domain.example",
                                port = 587,
                                connectionSecurity = 2,
                                authenticationType = 1,
                                username = "username",
                                password = null,
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
            ),
        )
    }

    @Test
    fun `one account, one identity, no account name, with passwords`() {
        val payload = """[1,[1,1],""" +
            """[0,"imap.domain.example",993,3,2,"username","","imap-password"],""" +
            """[[[0,"smtp.domain.example",587,2,1,"username","smtp-password"],""" +
            """["user@domain.example","Firstname Lastname"]]]]"""

        val result = parser.parse(payload)

        assertThat(result).isNotNull().isEqualTo(
            QrCodeData(
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
                            authenticationType = 2,
                            username = "username",
                            accountName = "",
                            password = "imap-password",
                        ),
                        outgoingServers = listOf(
                            QrCodeData.OutgoingServer(
                                protocol = 0,
                                hostname = "smtp.domain.example",
                                port = 587,
                                connectionSecurity = 2,
                                authenticationType = 1,
                                username = "username",
                                password = "smtp-password",
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
            ),
        )
    }

    @Test
    fun `one account, one identity, with account name, with passwords`() {
        val payload = """[1,[1,1],""" +
            """[0,"imap.domain.example",993,3,2,"username","Personal","imap-password"],""" +
            """[[[0,"smtp.domain.example",587,2,1,"username","smtp-password"],""" +
            """["user@domain.example","Firstname Lastname"]]]]"""

        val result = parser.parse(payload)

        assertThat(result).isNotNull().isEqualTo(
            QrCodeData(
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
                            authenticationType = 2,
                            username = "username",
                            accountName = "Personal",
                            password = "imap-password",
                        ),
                        outgoingServers = listOf(
                            QrCodeData.OutgoingServer(
                                protocol = 0,
                                hostname = "smtp.domain.example",
                                port = 587,
                                connectionSecurity = 2,
                                authenticationType = 1,
                                username = "username",
                                password = "smtp-password",
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
            ),
        )
    }

    @Test
    fun `one account, two identities`() {
        val payload = """[1,[1,1],""" +
            """[0,"imap.domain.example",993,3,2,"username","","imap-password"],""" +
            """[[[0,"smtp.domain.example",587,2,1,"username","smtp-password"],""" +
            """["user@domain.example","Firstname Lastname"],""" +
            """["alias@domain.example","Nickname Lastname"]]]]"""

        val result = parser.parse(payload)

        assertThat(result).isNotNull().isEqualTo(
            QrCodeData(
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
                            authenticationType = 2,
                            username = "username",
                            accountName = "",
                            password = "imap-password",
                        ),
                        outgoingServers = listOf(
                            QrCodeData.OutgoingServer(
                                protocol = 0,
                                hostname = "smtp.domain.example",
                                port = 587,
                                connectionSecurity = 2,
                                authenticationType = 1,
                                username = "username",
                                password = "smtp-password",
                                identities = listOf(
                                    QrCodeData.Identity(
                                        emailAddress = "user@domain.example",
                                        displayName = "Firstname Lastname",
                                    ),
                                    QrCodeData.Identity(
                                        emailAddress = "alias@domain.example",
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
    fun `two accounts, two identities each`() {
        val payload = """[1,[1,1],""" +
            """[0,"imap.domain.example",993,3,2,"username","","imap-password"],""" +
            """[[[0,"smtp.domain.example",587,2,1,"username","smtp-password"],""" +
            """["user@domain.example","Firstname Lastname"],""" +
            """["alias@domain.example","Nickname"]]],""" +
            """[0,"imap.company.example",143,2,1,"user@company.example","","company-password"],""" +
            """[[[0,"smtp.company.example",465,3,1,"user@company.example","company-password"],""" +
            """["user@company.example","Firstname Lastname"],""" +
            """["alias@company.example","Nickname Lastname"]]]]"""

        val result = parser.parse(payload)

        assertThat(result).isNotNull().isEqualTo(
            QrCodeData(
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
                            authenticationType = 2,
                            username = "username",
                            accountName = "",
                            password = "imap-password",
                        ),
                        outgoingServers = listOf(
                            QrCodeData.OutgoingServer(
                                protocol = 0,
                                hostname = "smtp.domain.example",
                                port = 587,
                                connectionSecurity = 2,
                                authenticationType = 1,
                                username = "username",
                                password = "smtp-password",
                                identities = listOf(
                                    QrCodeData.Identity(
                                        emailAddress = "user@domain.example",
                                        displayName = "Firstname Lastname",
                                    ),
                                    QrCodeData.Identity(
                                        emailAddress = "alias@domain.example",
                                        displayName = "Nickname",
                                    ),
                                ),
                            ),
                        ),
                    ),
                    QrCodeData.Account(
                        incomingServer = QrCodeData.IncomingServer(
                            protocol = 0,
                            hostname = "imap.company.example",
                            port = 143,
                            connectionSecurity = 2,
                            authenticationType = 1,
                            username = "user@company.example",
                            accountName = "",
                            password = "company-password",
                        ),
                        outgoingServers = listOf(
                            QrCodeData.OutgoingServer(
                                protocol = 0,
                                hostname = "smtp.company.example",
                                port = 465,
                                connectionSecurity = 3,
                                authenticationType = 1,
                                username = "user@company.example",
                                password = "company-password",
                                identities = listOf(
                                    QrCodeData.Identity(
                                        emailAddress = "user@company.example",
                                        displayName = "Firstname Lastname",
                                    ),
                                    QrCodeData.Identity(
                                        emailAddress = "alias@company.example",
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
    fun `additional array entries in incoming server array, outgoing server array, and identity array`() {
        val payload = """[1,[1,1],""" +
            """[0,"imap.domain.example",993,3,2,"username","","password","extra"],""" +
            """[[[0,"smtp.domain.example",587,2,1,"username","password","extra"],""" +
            """["user@domain.example","Firstname Lastname","extra"]]]]"""

        val result = parser.parse(payload)

        assertThat(result).isNotNull().isEqualTo(
            QrCodeData(
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
                            authenticationType = 2,
                            username = "username",
                            accountName = "",
                            password = "password",
                        ),
                        outgoingServers = listOf(
                            QrCodeData.OutgoingServer(
                                protocol = 0,
                                hostname = "smtp.domain.example",
                                port = 587,
                                connectionSecurity = 2,
                                authenticationType = 1,
                                username = "username",
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
            ),
        )
    }

    @Test
    fun `additional array entries in meta array`() {
        val payload = """[1,[1,1,"extra"],""" +
            """[0,"imap.domain.example",993,3,2,"username","","password","extra"],""" +
            """[[[0,"smtp.domain.example",587,2,1,"username","password","extra"],""" +
            """["user@domain.example","Firstname Lastname","extra"]]]]"""

        val result = parser.parse(payload)

        assertThat(result).isNotNull()
    }

    @Test
    fun `URL instead of valid payload`() {
        val payload = "https://domain.example/path"

        val result = parser.parse(payload)

        assertThat(result).isNull()
    }

    @Test
    fun `incomplete payload`() {
        val payload = "[1,["

        val result = parser.parse(payload)

        assertThat(result).isNull()
    }
}
