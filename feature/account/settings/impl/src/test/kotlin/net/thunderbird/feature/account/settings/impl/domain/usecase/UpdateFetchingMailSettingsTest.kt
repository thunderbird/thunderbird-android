package net.thunderbird.feature.account.settings.impl.domain.usecase

import androidx.compose.ui.viewinterop.NoOpUpdate
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import kotlin.test.Test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.android.account.DeletePolicy
import net.thunderbird.core.android.account.Expunge
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountRepository
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto
import net.thunderbird.feature.account.storage.profile.ProfileDto

internal class UpdateFetchingMailSettingsTest {

    private fun createAccount(): LegacyAccount {
        val id = AccountIdFactory.create()

        return LegacyAccount(
            isSensitiveDebugLoggingEnabled = { true },
            id = id,
            name = "Test Account",
            email = "test@example.com",

            profile = ProfileDto(
                id = id,
                name = "Test Account",
                color = 0xFF0000,
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
                    description = "Test Identity",
                ),
            ),

            incomingServerSettings = ServerSettings(
                type = "imap",
                host = "imap.test.com",
                port = 993,
                connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
                authenticationType = AuthType.PLAIN,
                username = "user",
                password = "pass",
                clientCertificateAlias = null,
            ),

            outgoingServerSettings = ServerSettings(
                type = "smtp",
                host = "smtp.test.com",
                port = 465,
                connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
                authenticationType = AuthType.PLAIN,
                username = "user",
                password = "pass",
                clientCertificateAlias = null,
            ),

            displayCount = 10,
            maximumPolledMessageAge = 7,
            maximumAutoDownloadMessageSize = 1024,
            automaticCheckIntervalMinutes = 15,
            isSyncRemoteDeletions = false,
            isMarkMessageAsReadOnDelete = false,
            deletePolicy = DeletePolicy.NEVER,
            expungePolicy = Expunge.EXPUNGE_IMMEDIATELY,
            maxPushFolders = 5,
            idleRefreshMinutes = 15,
        )
    }

    private fun repoWith(account: LegacyAccount) = object : LegacyAccountRepository {
        var updated: LegacyAccount? = null

        override fun getById(id: AccountId): Flow<LegacyAccount?> = flowOf(account)

        override suspend fun update(account: LegacyAccount) {
            updated = account
        }
    }

    @Test
    fun `should return NotFound when account missing`() = runTest {
        val repo = object : LegacyAccountRepository {
            override fun getById(id: AccountId): Flow<LegacyAccount?> = emptyFlow()
            override suspend fun update(account: LegacyAccount) {
                NoOpUpdate
            }
        }

        val useCase = UpdateFetchingMailSettings(repo)

        val result = useCase(
            AccountIdFactory.create(),
            AccountSettingsDomainContract.UpdateFetchingMailSettingsCommand.UpdateLocalFolderSize(10),
        )

        assertThat(result).isInstanceOf(Outcome.Failure::class)
    }

    @Test
    fun `should update local folder size`() = runTest {
        val account = createAccount()
        val repo = repoWith(account)

        val useCase = UpdateFetchingMailSettings(repo)

        val result = useCase(
            account.id,
            AccountSettingsDomainContract.UpdateFetchingMailSettingsCommand.UpdateLocalFolderSize(25),
        )

        assertThat(result).isInstanceOf(Outcome.Success::class)

        val updated = requireNotNull(repo.updated)
        assertThat(updated.displayCount).isEqualTo(25)
    }

    @Test
    fun `should update sync message age`() = runTest {
        val account = createAccount()
        val repo = repoWith(account)

        val useCase = UpdateFetchingMailSettings(repo)

        useCase(
            account.id,
            AccountSettingsDomainContract.UpdateFetchingMailSettingsCommand.UpdateSyncMessageFrom(14),
        )

        val updated = requireNotNull(repo.updated)
        assertThat(updated.maximumPolledMessageAge).isEqualTo(14)
    }

    @Test
    fun `should update delete policy ON_DELETE`() = runTest {
        val account = createAccount()
        val repo = repoWith(account)

        val useCase = UpdateFetchingMailSettings(repo)

        useCase(
            account.id,
            AccountSettingsDomainContract.UpdateFetchingMailSettingsCommand.UpdateDeletePolicy("ON_DELETE"),
        )

        val updated = requireNotNull(repo.updated)
        assertThat(updated.deletePolicy).isEqualTo(DeletePolicy.ON_DELETE)
    }

    @Test
    fun `should update expunge policy MANUAL`() = runTest {
        val account = createAccount()
        val repo = repoWith(account)

        val useCase = UpdateFetchingMailSettings(repo)

        useCase(
            account.id,
            AccountSettingsDomainContract
                .UpdateFetchingMailSettingsCommand.UpdateExpungePolicy("EXPUNGE_MANUALLY"),
        )

        val updated = requireNotNull(repo.updated)
        assertThat(updated.expungePolicy).isEqualTo(Expunge.EXPUNGE_MANUALLY)
    }

    @Test
    fun `should update push folder limit`() = runTest {
        val account = createAccount()
        val repo = repoWith(account)

        val useCase = UpdateFetchingMailSettings(repo)

        useCase(
            account.id,
            AccountSettingsDomainContract.UpdateFetchingMailSettingsCommand.UpdateMaxPushFolders(250),
        )

        val updated = requireNotNull(repo.updated)
        assertThat(updated.maxPushFolders).isEqualTo(250)
    }

    @Test
    fun `should update idle refresh frequency`() = runTest {
        val account = createAccount()
        val repo = repoWith(account)

        val useCase = UpdateFetchingMailSettings(repo)

        useCase(
            account.id,
            AccountSettingsDomainContract
                .UpdateFetchingMailSettingsCommand.UpdateIdleRefreshMinutes(
                    60,
                ),
        )

        val updated = requireNotNull(repo.updated)
        assertThat(updated.idleRefreshMinutes).isEqualTo(60)
    }

    @Test
    fun `should update sync server deletions`() = runTest {
        val account = createAccount()
        val repo = repoWith(account)

        val useCase = UpdateFetchingMailSettings(repo)

        useCase(
            account.id,
            AccountSettingsDomainContract.UpdateFetchingMailSettingsCommand.UpdateSyncServerDeletions(true),
        )

        val updated = requireNotNull(repo.updated)
        assertThat(updated.isSyncRemoteDeletions).isEqualTo(true)
    }

    @Test
    fun `should update mark as read when deleted`() = runTest {
        val account = createAccount()
        val repo = repoWith(account)

        val useCase = UpdateFetchingMailSettings(repo)

        useCase(
            account.id,
            AccountSettingsDomainContract.UpdateFetchingMailSettingsCommand.UpdateMarkAsReadWhenDeleted(true),
        )

        val updated = requireNotNull(repo.updated)
        assertThat(updated.isMarkMessageAsReadOnDelete).isEqualTo(true)
    }

    @Test
    fun `should update fetch message up to`() = runTest {
        val account = createAccount()
        val repo = repoWith(account)

        val useCase = UpdateFetchingMailSettings(repo)

        useCase(
            account.id,
            AccountSettingsDomainContract.UpdateFetchingMailSettingsCommand.UpdateFetchMessageUpTo(4096),
        )

        val updated = requireNotNull(repo.updated)
        assertThat(updated.maximumAutoDownloadMessageSize).isEqualTo(4096)
    }

    @Test
    fun `should update folder poll frequency`() = runTest {
        val account = createAccount()
        val repo = repoWith(account)

        val useCase = UpdateFetchingMailSettings(repo)

        useCase(
            account.id,
            AccountSettingsDomainContract.UpdateFetchingMailSettingsCommand.UpdateFolderPollFrequency(60),
        )

        val updated = requireNotNull(repo.updated)
        assertThat(updated.automaticCheckIntervalMinutes).isEqualTo(60)
    }
}
