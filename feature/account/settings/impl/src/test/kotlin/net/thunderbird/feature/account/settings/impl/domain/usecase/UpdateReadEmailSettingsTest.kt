package net.thunderbird.feature.account.settings.impl.domain.usecase

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import kotlin.test.Test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountRepository
import net.thunderbird.core.android.account.ShowPictures
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto
import net.thunderbird.feature.account.storage.profile.ProfileDto

internal class UpdateReadEmailSettingsTest {

    @Test
    fun `should return NotFound when account does not exist`() = runTest {
        val accountId = AccountIdFactory.create()

        val repository = object : LegacyAccountRepository {
            override fun getById(id: AccountId): Flow<LegacyAccount?> = emptyFlow()
            override suspend fun update(account: LegacyAccount) = Unit
        }

        val testSubject = UpdateReadEmailSettings(repository)

        val result = testSubject(
            accountId,
            AccountSettingsDomainContract.UpdateReadMessageSettingsCommand
                .UpdateIsMarkMessageAsReadOnView(true),
        )

        assertThat(result).isInstanceOf(Outcome.Failure::class)
    }

    @Test
    fun `should update mark as read on view`() = runTest {
        val accountId = AccountIdFactory.create()

        var updatedAccount: LegacyAccount? = null

        val existing = createLegacyAccount(accountId)

        val repository = object : LegacyAccountRepository {
            override fun getById(id: AccountId) =
                flowOf(existing)

            override suspend fun update(account: LegacyAccount) {
                updatedAccount = account
            }
        }

        val testSubject = UpdateReadEmailSettings(repository)

        val result = testSubject(
            accountId,
            AccountSettingsDomainContract.UpdateReadMessageSettingsCommand
                .UpdateIsMarkMessageAsReadOnView(true),
        )

        assertThat(result).isInstanceOf(Outcome.Success::class)
        assertThat(updatedAccount).isNotNull()
        assertThat(updatedAccount!!.isMarkMessageAsReadOnView).isTrue()
    }

    @Test
    fun `should update show pictures to ALWAYS`() = runTest {
        val accountId = AccountIdFactory.create()

        var updatedAccount: LegacyAccount? = null

        val existing = createLegacyAccount(accountId)

        val repository = object : LegacyAccountRepository {
            override fun getById(id: AccountId) =
                flowOf(existing)

            override suspend fun update(account: LegacyAccount) {
                updatedAccount = account
            }
        }

        val testSubject = UpdateReadEmailSettings(repository)

        val result = testSubject(
            accountId,
            AccountSettingsDomainContract.UpdateReadMessageSettingsCommand
                .UpdateShowPictures(ShowPictures.ALWAYS.name),
        )

        assertThat(result).isInstanceOf(Outcome.Success::class)
        assertThat(updatedAccount).isNotNull()
        assertThat(updatedAccount!!.showPictures)
            .isEqualTo(ShowPictures.ALWAYS)
    }

    @Test
    fun `should update show pictures to ONLY_FROM_CONTACTS`() = runTest {
        val accountId = AccountIdFactory.create()

        var updatedAccount: LegacyAccount? = null

        val existing = createLegacyAccount(accountId)

        val repository = object : LegacyAccountRepository {
            override fun getById(id: AccountId) =
                flowOf(existing)

            override suspend fun update(account: LegacyAccount) {
                updatedAccount = account
            }
        }

        val testSubject = UpdateReadEmailSettings(repository)

        val result = testSubject(
            accountId,
            AccountSettingsDomainContract.UpdateReadMessageSettingsCommand
                .UpdateShowPictures(ShowPictures.ONLY_FROM_CONTACTS.name),
        )

        assertThat(result).isInstanceOf(Outcome.Success::class)
        assertThat(updatedAccount!!.showPictures)
            .isEqualTo(ShowPictures.ONLY_FROM_CONTACTS)
    }

    private companion object Companion {
        fun createLegacyAccount(
            id: AccountId,
            displayName: String = "AccountProfileName",
            color: Int = 0xFF0000,
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
    }
}
