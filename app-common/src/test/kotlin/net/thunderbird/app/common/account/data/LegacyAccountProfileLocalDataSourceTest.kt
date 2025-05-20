package net.thunderbird.app.common.account.data

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountWrapper
import net.thunderbird.feature.account.api.AccountId
import net.thunderbird.feature.account.api.profile.AccountProfile
import org.junit.Test

class LegacyAccountProfileLocalDataSourceTest {

    @Test
    fun `getById should return account profile`() = runTest {
        // arrange
        val accountId = AccountId.create()
        val legacyAccount = createLegacyAccount(accountId.value)
        val accountProfile = createAccountProfile(accountId)

        val testSubject = CommonAccountProfileLocalDataSource(
            accountManager = FakeLegacyAccountWrapperManager(
                initialAccounts = listOf(
                    legacyAccount,
                ),
            ),
        )

        // act & assert
        testSubject.getById(accountId).test {
            assertThat(awaitItem()).isEqualTo(accountProfile)
        }
    }

    @Test
    fun `getById should return null when account is not found`() = runTest {
        // arrange
        val accountId = AccountId.create()

        val testSubject = CommonAccountProfileLocalDataSource(
            accountManager = FakeLegacyAccountWrapperManager(),
        )

        // act & assert
        testSubject.getById(accountId).test {
            assertThat(awaitItem()).isEqualTo(null)
        }
    }

    @Test
    fun `update should save account profile`() = runTest {
        // arrange
        val accountId = AccountId.create()
        val legacyAccount = createLegacyAccount(accountId.value)
        val accountProfile = createAccountProfile(accountId)

        val updatedName = "updatedName"
        val updatedAccountProfile = accountProfile.copy(name = updatedName)

        val accountManager = FakeLegacyAccountWrapperManager(
            initialAccounts = listOf(
                legacyAccount,
            ),
        )

        val testSubject = CommonAccountProfileLocalDataSource(
            accountManager = accountManager,
        )

        // act & assert
        testSubject.getById(accountId).test {
            assertThat(awaitItem()).isEqualTo(accountProfile)

            testSubject.update(updatedAccountProfile)

            assertThat(awaitItem()).isEqualTo(updatedAccountProfile)
        }
    }

    private companion object {
        const val NAME = "name"
        const val COLOR = 0xFF333333.toInt()

        fun createLegacyAccount(
            accountId: String,
            displayName: String = NAME,
            color: Int = COLOR,
        ): LegacyAccountWrapper {
            return LegacyAccountWrapper.from(
                LegacyAccount(
                    uuid = accountId,
                    isSensitiveDebugLoggingEnabled = { true },
                ).apply {
                    identities = ArrayList()

                    val identity = Identity(
                        signatureUse = false,
                        description = "Demo User",
                    )
                    identities.add(identity)

                    name = displayName
                    chipColor = color
                    email = "demo@example.com"

                    incomingServerSettings = ServerSettings(
                        type = "imap",
                        host = "imap.example.com",
                        port = 993,
                        connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
                        authenticationType = AuthType.PLAIN,
                        username = "test",
                        password = "password",
                        clientCertificateAlias = null,
                    )
                    outgoingServerSettings = ServerSettings(
                        type = "smtp",
                        host = "smtp.example.com",
                        port = 465,
                        connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
                        authenticationType = AuthType.PLAIN,
                        username = "test",
                        password = "password",
                        clientCertificateAlias = null,
                    )
                },
            )
        }

        fun createAccountProfile(
            accountId: AccountId,
            name: String = NAME,
            color: Int = COLOR,
        ): AccountProfile {
            return AccountProfile(
                accountId = accountId,
                name = name,
                color = color,
            )
        }
    }
}
