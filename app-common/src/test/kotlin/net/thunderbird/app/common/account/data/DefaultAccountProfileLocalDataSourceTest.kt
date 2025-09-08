package net.thunderbird.app.common.account.data

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import kotlinx.coroutines.test.runTest
import net.thunderbird.account.fake.FakeAccountProfileData.PROFILE_COLOR
import net.thunderbird.account.fake.FakeAccountProfileData.PROFILE_NAME
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.profile.AccountAvatar
import net.thunderbird.feature.account.profile.AccountProfile
import net.thunderbird.feature.account.storage.legacy.mapper.DefaultAccountAvatarDataMapper
import net.thunderbird.feature.account.storage.legacy.mapper.DefaultAccountProfileDataMapper
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto
import net.thunderbird.feature.account.storage.profile.ProfileDto
import org.junit.Test

class DefaultAccountProfileLocalDataSourceTest {

    @Test
    fun `getById should return account profile`() = runTest {
        // arrange
        val accountId = AccountIdFactory.create()
        val legacyAccount = createLegacyAccount(accountId)
        val accountProfile = createAccountProfile(accountId)
        val testSubject = createTestSubject(legacyAccount)

        // act & assert
        testSubject.getById(accountId).test {
            assertThat(awaitItem()).isEqualTo(accountProfile)
        }
    }

    @Test
    fun `getById should return null when account is not found`() = runTest {
        // arrange
        val accountId = AccountIdFactory.create()
        val testSubject = createTestSubject(null)

        // act & assert
        testSubject.getById(accountId).test {
            assertThat(awaitItem()).isEqualTo(null)
        }
    }

    @Test
    fun `update should save account profile`() = runTest {
        // arrange
        val accountId = AccountIdFactory.create()
        val legacyAccount = createLegacyAccount(accountId)
        val accountProfile = createAccountProfile(accountId)

        val updatedName = "updatedName"
        val updatedAccountProfile = accountProfile.copy(name = updatedName)

        val testSubject = createTestSubject(legacyAccount)

        // act & assert
        testSubject.getById(accountId).test {
            assertThat(awaitItem()).isEqualTo(accountProfile)

            testSubject.update(updatedAccountProfile)

            assertThat(awaitItem()).isEqualTo(updatedAccountProfile)
        }
    }

    private companion object Companion {
        fun createLegacyAccount(
            id: AccountId,
            displayName: String = PROFILE_NAME,
            color: Int = PROFILE_COLOR,
        ): LegacyAccount {
            return LegacyAccount(
                isSensitiveDebugLoggingEnabled = { true },
                id = id,
                name = displayName,
                email = "demo@example.com",
                profile = ProfileDto(
                    id = id,
                    name = displayName,
                    color = color,
                    avatar = AvatarDto(
                        avatarType = AvatarTypeDto.ICON,
                        avatarMonogram = null,
                        avatarImageUri = null,
                        avatarIconName = "star",
                    ),
                ),
                identities = listOf(
                    Identity(
                        signatureUse = false,
                        description = "Demo User",
                    ),
                ),
                incomingServerSettings = ServerSettings(
                    type = "imap",
                    host = "imap.example.com",
                    port = 993,
                    connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
                    authenticationType = AuthType.PLAIN,
                    username = "test",
                    password = "password",
                    clientCertificateAlias = null,
                ),
                outgoingServerSettings = ServerSettings(
                    type = "smtp",
                    host = "smtp.example.com",
                    port = 465,
                    connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
                    authenticationType = AuthType.PLAIN,
                    username = "test",
                    password = "password",
                    clientCertificateAlias = null,
                ),
            )
        }

        private fun createAccountProfile(
            accountId: AccountId,
            name: String = PROFILE_NAME,
            color: Int = PROFILE_COLOR,
        ): AccountProfile {
            return AccountProfile(
                id = accountId,
                name = name,
                color = color,
                avatar = AccountAvatar.Icon(
                    name = "star",
                ),
            )
        }

        private fun createTestSubject(
            legacyAccount: LegacyAccount?,
        ): DefaultAccountProfileLocalDataSource {
            return DefaultAccountProfileLocalDataSource(
                accountManager = FakeLegacyAccountWrapperManager(
                    initialAccounts = if (legacyAccount != null) {
                        listOf(legacyAccount)
                    } else {
                        emptyList()
                    },
                ),
                dataMapper = DefaultAccountProfileDataMapper(
                    avatarMapper = DefaultAccountAvatarDataMapper(),
                ),
            )
        }
    }
}
