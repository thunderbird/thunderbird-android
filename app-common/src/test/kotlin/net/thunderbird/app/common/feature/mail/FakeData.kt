package net.thunderbird.app.common.feature.mail

import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import net.thunderbird.account.fake.FakeAccountData
import net.thunderbird.core.android.account.DeletePolicy
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto
import net.thunderbird.feature.account.storage.profile.ProfileDto
import net.thunderbird.feature.mail.account.api.BaseAccount

object FakeData {

    val legacyAccountDto = LegacyAccountDto(uuid = FakeAccountData.ACCOUNT_ID_RAW).apply {
        identities = mutableListOf(Identity(email = "user@example.com"))
        email = "user@example.com"
        incomingServerSettings = ServerSettings(
            type = Protocols.IMAP,
            host = "imap.example.com",
            port = 993,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "user",
            password = "pass",
            clientCertificateAlias = null,
        )
        outgoingServerSettings = ServerSettings(
            type = Protocols.IMAP,
            host = "smtp.example.com",
            port = 465,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "user",
            password = "pass",
            clientCertificateAlias = null,
        )
    }

    val legacyAccount = LegacyAccount(
        id = FakeAccountData.ACCOUNT_ID,
        name = "user@example.com",
        profile = ProfileDto(
            id = FakeAccountData.ACCOUNT_ID,
            name = "user@example.com",
            color = 0,
            avatar = AvatarDto(
                avatarType = AvatarTypeDto.MONOGRAM,
                avatarMonogram = "us",
                avatarImageUri = null,
                avatarIconName = null,
            ),
        ),
        identities = listOf(Identity(email = "user@example.com")),
        email = "user@example.com",
        deletePolicy = DeletePolicy.NEVER,
        incomingServerSettings = ServerSettings(
            type = Protocols.IMAP,
            host = "imap.example.com",
            port = 993,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "user",
            password = "pass",
            clientCertificateAlias = null,
        ),
        outgoingServerSettings = ServerSettings(
            type = Protocols.IMAP,
            host = "smtp.example.com",
            port = 465,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "user",
            password = "pass",
            clientCertificateAlias = null,
        ),
    )

    val unsupportedAccount = object : BaseAccount {
        override val uuid: String = "x"
        override val name: String? = "n"
        override val email: String = "e@example.com"
    }
}
