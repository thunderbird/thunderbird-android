package com.fsck.k9

import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import net.thunderbird.account.fake.FakeAccountData.ACCOUNT_ID
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto
import net.thunderbird.feature.account.storage.profile.ProfileDto

object FakeLegacyAccount {

    fun create(
        id: AccountId = ACCOUNT_ID,
        name: String = "test",
        email: String = "test@example.com",
        identities: List<Identity> = emptyList(),
        trashFolderId: Long? = null,
        spamFolderId: Long? = null,
        archiveFolderId: Long? = null,
    ): LegacyAccount = LegacyAccount(
        id = id,
        name = name,
        email = email,
        profile = ProfileDto(
            id = id,
            name = name,
            color = -1,
            avatar = AvatarDto(
                id = id,
                avatarType = AvatarTypeDto.MONOGRAM,
                avatarMonogram = "TE",
                avatarImageUri = null,
                avatarIconName = null,
            ),
        ),
        incomingServerSettings = ServerSettings(
            type = Protocols.IMAP,
            host = "host",
            port = 993,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "user",
            password = "pass",
            clientCertificateAlias = null,
        ),
        outgoingServerSettings = ServerSettings(
            type = Protocols.SMTP,
            host = "host",
            port = 465,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "user",
            password = "pass",
            clientCertificateAlias = null,
        ),
        identities = identities,
        trashFolderId = trashFolderId,
        spamFolderId = spamFolderId,
        archiveFolderId = archiveFolderId,
    )
}
