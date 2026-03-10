package net.thunderbird.feature.mail.message.list.internal.fakes

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

@Suppress("TestFunctionName", "ktlint:standard:function-naming")
fun FakeLegacyAccount(
    id: AccountId = AccountIdFactory.create(),
    name: String = "fake",
    email: String = "fake@mail.com",
    incomingServerType: String = Protocols.IMAP,
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
            avatarType = AvatarTypeDto.MONOGRAM,
            avatarMonogram = "FA",
            avatarImageUri = null,
            avatarIconName = null,
        ),
    ),
    incomingServerSettings = ServerSettings(
        type = incomingServerType,
        host = "fake",
        port = -1,
        connectionSecurity = ConnectionSecurity.NONE,
        authenticationType = AuthType.NONE,
        username = "fake",
        password = "fake",
        clientCertificateAlias = null,
    ),
    outgoingServerSettings = ServerSettings(
        type = "fake",
        host = "fake",
        port = -1,
        connectionSecurity = ConnectionSecurity.NONE,
        authenticationType = AuthType.NONE,
        username = "fake",
        password = "fake",
        clientCertificateAlias = null,
    ),
    identities = listOf(Identity(email = email)),
    archiveFolderId = archiveFolderId,
)
