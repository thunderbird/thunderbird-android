package net.thunderbird.core.android.account

import assertk.all
import assertk.assertThat
import assertk.assertions.doesNotContain
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import kotlin.test.Test
import net.thunderbird.account.fake.FakeAccountData.ACCOUNT_ID
import net.thunderbird.account.fake.FakeAccountProfileData.PROFILE_COLOR
import net.thunderbird.account.fake.FakeAccountProfileData.PROFILE_NAME
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto
import net.thunderbird.feature.account.storage.profile.ProfileDto

class LegacyAccountTest {
    @Test
    fun `sensitive information should not appear in toString`() {
        assertThat(validAccount.toString()).all {
            doesNotContain(OAUTH_STATE)
            doesNotContain(PASSWORD)
            doesNotContain(EMAIL)
        }
    }

    private val validAccount get() = LegacyAccount(
        id = ACCOUNT_ID,
        name = PROFILE_NAME,
        email = EMAIL,
        profile = ProfileDto(
            id = ACCOUNT_ID,
            name = PROFILE_NAME,
            color = PROFILE_COLOR,
            avatar = AvatarDto(
                accountId = ACCOUNT_ID,
                avatarType = AvatarTypeDto.MONOGRAM,
                avatarMonogram = null,
                avatarImageUri = null,
                avatarIconName = null,
            ),
        ),
        incomingServerSettings = ServerSettings(
            type = "imap",
            host = "imap.domain.example",
            port = 993,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "user",
            password = PASSWORD,
            clientCertificateAlias = null,
        ),
        outgoingServerSettings = ServerSettings(
            type = "smtp",
            host = "smtp.domain.example",
            port = 465,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "user",
            password = PASSWORD,
            clientCertificateAlias = null,
        ),
        oAuthState = OAUTH_STATE,
        identities = listOf(
            Identity(
                email = EMAIL,
                name = "identityName",
            ),
        ),
    )

    private companion object {
        const val EMAIL = "test@domain.example"
        const val PASSWORD = "123456"
        const val OAUTH_STATE = "{\"refreshToken\":\"secret-refresh-token\"}"
    }
}
