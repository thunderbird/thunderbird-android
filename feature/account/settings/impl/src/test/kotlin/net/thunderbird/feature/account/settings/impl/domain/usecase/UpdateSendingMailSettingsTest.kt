package net.thunderbird.feature.account.settings.impl.domain.usecase

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import kotlin.test.Test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.thunderbird.components.core.outcome.Outcome
import net.thunderbird.core.android.account.DeletePolicy
import net.thunderbird.core.android.account.Expunge
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountRepository
import net.thunderbird.core.android.account.MessageFormat
import net.thunderbird.core.android.account.QuoteStyle
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto
import net.thunderbird.feature.account.storage.profile.ProfileDto

internal class UpdateSendingMailSettingsTest {

    private fun createAccount(): LegacyAccount {
        val id = AccountIdFactory.create()

        return LegacyAccount(
            id = id,
            name = "Test Account",
            email = "test@example.com",
            profile = ProfileDto(
                id = id,
                name = "Test Account",
                color = 0xFF0000,
                avatar = AvatarDto(
                    accountId = id,
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

    private fun repoWith(account: LegacyAccount) =
        object : LegacyAccountRepository {
            var updated: LegacyAccount? = null

            override fun observeById(id: AccountId): Flow<LegacyAccount?> =
                flowOf(account)

            override suspend fun update(
                accountId: AccountId,
                transform: (LegacyAccount) -> LegacyAccount,
            ): LegacyAccount? =
                observeById(accountId).firstOrNull()?.let(transform)?.also { updated = it }
        }

    @Test
    fun `should return NotFound when account missing`() = runTest {
        val repo = object : LegacyAccountRepository {
            override fun observeById(id: AccountId): Flow<LegacyAccount?> =
                emptyFlow()

            override suspend fun update(
                accountId: AccountId,
                transform: (LegacyAccount) -> LegacyAccount,
            ): LegacyAccount? =
                observeById(accountId).firstOrNull()?.let(transform)
        }

        val useCase = UpdateSendingMailSettings(repo)

        val result = useCase(
            AccountIdFactory.create(),
            AccountSettingsDomainContract.UpdateSendingMailSettingsCommand
                .UpdateAlwaysShowCcBcc(true),
        )

        assertThat(result).isInstanceOf(Outcome.Failure::class)
    }

    @Test
    fun `should update message format to TEXT`() = runTest {
        val account = createAccount()
        val repo = repoWith(account)
        val useCase = UpdateSendingMailSettings(repo)

        val result = useCase(
            account.id,
            AccountSettingsDomainContract.UpdateSendingMailSettingsCommand
                .UpdateMessageFormat("TEXT"),
        )

        assertThat(result).isInstanceOf(Outcome.Success::class)
        assertThat(requireNotNull(repo.updated).messageFormat)
            .isEqualTo(MessageFormat.TEXT)
    }

    @Test
    fun `should update message format to HTML`() = runTest {
        val account = createAccount()
        val repo = repoWith(account)
        val useCase = UpdateSendingMailSettings(repo)

        useCase(
            account.id,
            AccountSettingsDomainContract.UpdateSendingMailSettingsCommand
                .UpdateMessageFormat("HTML"),
        )

        assertThat(requireNotNull(repo.updated).messageFormat)
            .isEqualTo(MessageFormat.HTML)
    }

    @Test
    fun `should update message format to AUTO`() = runTest {
        val account = createAccount()
        val repo = repoWith(account)
        val useCase = UpdateSendingMailSettings(repo)

        useCase(
            account.id,
            AccountSettingsDomainContract.UpdateSendingMailSettingsCommand
                .UpdateMessageFormat("AUTO"),
        )

        assertThat(requireNotNull(repo.updated).messageFormat)
            .isEqualTo(MessageFormat.AUTO)
    }

    @Test
    fun `should update always show CC and BCC`() = runTest {
        val account = createAccount()
        val repo = repoWith(account)
        val useCase = UpdateSendingMailSettings(repo)

        useCase(
            account.id,
            AccountSettingsDomainContract.UpdateSendingMailSettingsCommand
                .UpdateAlwaysShowCcBcc(true),
        )

        assertThat(requireNotNull(repo.updated).isAlwaysShowCcBcc)
            .isEqualTo(true)
    }

    @Test
    fun `should update read receipt`() = runTest {
        val account = createAccount()
        val repo = repoWith(account)
        val useCase = UpdateSendingMailSettings(repo)

        useCase(
            account.id,
            AccountSettingsDomainContract.UpdateSendingMailSettingsCommand
                .UpdateReadReceipt(true),
        )

        assertThat(requireNotNull(repo.updated).isMessageReadReceipt)
            .isEqualTo(true)
    }

    @Test
    fun `should update reply quoting style to PREFIX`() = runTest {
        val account = createAccount()
        val repo = repoWith(account)
        val useCase = UpdateSendingMailSettings(repo)

        useCase(
            account.id,
            AccountSettingsDomainContract.UpdateSendingMailSettingsCommand
                .UpdateReplyQuotingStyle("PREFIX"),
        )

        assertThat(requireNotNull(repo.updated).quoteStyle)
            .isEqualTo(QuoteStyle.PREFIX)
    }

    @Test
    fun `should update reply quoting style to HEADER`() = runTest {
        val account = createAccount()
        val repo = repoWith(account)
        val useCase = UpdateSendingMailSettings(repo)

        useCase(
            account.id,
            AccountSettingsDomainContract.UpdateSendingMailSettingsCommand
                .UpdateReplyQuotingStyle("HEADER"),
        )

        assertThat(requireNotNull(repo.updated).quoteStyle)
            .isEqualTo(QuoteStyle.HEADER)
    }

    @Test
    fun `should update quote message when replying`() = runTest {
        val account = createAccount()
        val repo = repoWith(account)
        val useCase = UpdateSendingMailSettings(repo)

        useCase(
            account.id,
            AccountSettingsDomainContract.UpdateSendingMailSettingsCommand
                .UpdateQuoteMessageWhenReplying(false),
        )

        assertThat(requireNotNull(repo.updated).isDefaultQuotedTextShown)
            .isEqualTo(false)
    }

    @Test
    fun `should update reply after quoted text`() = runTest {
        val account = createAccount()
        val repo = repoWith(account)
        val useCase = UpdateSendingMailSettings(repo)

        useCase(
            account.id,
            AccountSettingsDomainContract.UpdateSendingMailSettingsCommand
                .UpdateReplyAfterQuotedText(false),
        )

        assertThat(requireNotNull(repo.updated).isReplyAfterQuote)
            .isEqualTo(false)
    }

    @Test
    fun `should update strip signature on reply`() = runTest {
        val account = createAccount()
        val repo = repoWith(account)
        val useCase = UpdateSendingMailSettings(repo)

        useCase(
            account.id,
            AccountSettingsDomainContract.UpdateSendingMailSettingsCommand
                .UpdateStripSignatureOnReply(false),
        )

        assertThat(requireNotNull(repo.updated).isStripSignature)
            .isEqualTo(false)
    }

    @Test
    fun `should update quoted text prefix`() = runTest {
        val account = createAccount()
        val repo = repoWith(account)
        val useCase = UpdateSendingMailSettings(repo)

        useCase(
            account.id,
            AccountSettingsDomainContract.UpdateSendingMailSettingsCommand
                .UpdateQuotedTextPrefix("> "),
        )

        assertThat(requireNotNull(repo.updated).quotePrefix)
            .isEqualTo("> ")
    }

    @Test
    fun `should update upload sent messages`() = runTest {
        val account = createAccount()
        val repo = repoWith(account)
        val useCase = UpdateSendingMailSettings(repo)

        useCase(
            account.id,
            AccountSettingsDomainContract.UpdateSendingMailSettingsCommand
                .UpdateUploadSentMessages(false),
        )

        assertThat(requireNotNull(repo.updated).isUploadSentMessages)
            .isEqualTo(false)
    }
}
