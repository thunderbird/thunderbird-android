package com.fsck.k9

import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto
import net.thunderbird.feature.account.storage.profile.ProfileDto

object FakeLegacyAccount {

    val ACCOUNT_ID = AccountIdFactory.create()
    const val ACCOUNT_EMAIL = "fake@examle.com"

    val ACCOUNT = LegacyAccount(
        id = ACCOUNT_ID,
        name = "fake",
        email = ACCOUNT_EMAIL,
        profile = ProfileDto(
            id = ACCOUNT_ID,
            name = "fake",
            color = -1,
            avatar = AvatarDto(
                id = ACCOUNT_ID,
                avatarType = AvatarTypeDto.MONOGRAM,
                avatarMonogram = "FA",
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
            username = "username",
            password = "password",
            clientCertificateAlias = null,
        ),
        outgoingServerSettings = ServerSettings(
            type = Protocols.SMTP,
            host = "host",
            port = 465,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "username",
            password = "password",
            clientCertificateAlias = null,
        ),
        identities = listOf(Identity(email = ACCOUNT_EMAIL)),
    )

    @JvmStatic
    @JvmOverloads
    fun create(
        id: AccountId = ACCOUNT_ID,
        sentFolderId: Long? = null,
        remoteSearchNumResults: Int = 0,
    ): LegacyAccount = ACCOUNT.copy(
        id = id,
        sentFolderId = sentFolderId,
        remoteSearchNumResults = remoteSearchNumResults,
    )
}
