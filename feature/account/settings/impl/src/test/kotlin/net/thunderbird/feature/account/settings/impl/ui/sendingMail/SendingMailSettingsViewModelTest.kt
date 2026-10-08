package net.thunderbird.feature.account.settings.impl.ui.sendingMail

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.thunderbird.components.core.outcome.Outcome
import net.thunderbird.components.ui.testing.coroutines.MainDispatcherHelper
import net.thunderbird.core.android.account.DeletePolicy
import net.thunderbird.core.android.account.Expunge
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.MessageFormat
import net.thunderbird.core.android.account.QuoteStyle
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.core.ui.setting.SettingValue.Select.SelectOption
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract

@OptIn(ExperimentalCoroutinesApi::class)
internal class SendingMailSettingsViewModelTest {

    private val mainDispatcher = MainDispatcherHelper()

    @BeforeTest
    fun setUp() {
        mainDispatcher.setUp()
    }

    @AfterTest
    fun tearDown() {
        mainDispatcher.tearDown()
    }

    private val resources = object :
        net.thunderbird.core.common.resources.StringsResourceManager {

        override fun stringResource(resourceId: Int): String =
            "string_$resourceId"

        override fun stringResource(
            resourceId: Int,
            vararg formatArgs: Any?,
        ): String = stringResource(resourceId)
    }

    private val optionsMapper = SendingMailSettingsOptionsMapper(resources)

    private fun defaultState() = SendingMailSettingContract.State(
        subtitle = null,
        messageFormat = SelectOption("TEXT") { "" },
        alwaysShowCcBcc = false,
        readReceipt = false,
        replyQuotingStyle = SelectOption("PREFIX") { "" },
        quoteMessageWhenReplying = false,
        replyAfterQuotedText = false,
        stripSignatureOnReply = false,
        quotedTextPrefix = ">",
        uploadSentMessages = false,
    )

    @Suppress("LongMethod")
    private fun dummyLegacyAccount(
        accountId: AccountId,
        isStripSignature: Boolean = true,
    ): LegacyAccount {
        return LegacyAccount(
            id = accountId,
            name = "Demo",
            email = "demo@example.com",
            displayCount = 500,
            maximumPolledMessageAge = 365,
            maximumAutoDownloadMessageSize = 5242880,
            automaticCheckIntervalMinutes = 720,
            isSyncRemoteDeletions = true,
            isMarkMessageAsReadOnDelete = true,
            deletePolicy = DeletePolicy.MARK_AS_READ,
            expungePolicy = Expunge.EXPUNGE_MANUALLY,
            maxPushFolders = 250,
            idleRefreshMinutes = 48,
            messageFormat = MessageFormat.HTML,
            isAlwaysShowCcBcc = true,
            isMessageReadReceipt = true,
            quoteStyle = QuoteStyle.HEADER,
            isDefaultQuotedTextShown = true,
            isReplyAfterQuote = true,
            isStripSignature = isStripSignature,
            quotePrefix = "> ",
            isUploadSentMessages = true,
            isSensitiveDebugLoggingEnabled = { true },
            profile = net.thunderbird.feature.account.storage.profile.ProfileDto(
                id = accountId,
                name = "Demo",
                color = 0xFF0000,
                avatar = net.thunderbird.feature.account.storage.profile.AvatarDto(
                    avatarType = net.thunderbird.feature.account.storage.profile.AvatarTypeDto.ICON,
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
            incomingServerSettings = com.fsck.k9.mail.ServerSettings(
                type = "imap",
                host = "imap.example.com",
                port = 993,
                connectionSecurity = com.fsck.k9.mail.ConnectionSecurity.SSL_TLS_REQUIRED,
                authenticationType = com.fsck.k9.mail.AuthType.PLAIN,
                username = "test",
                password = "pass",
                clientCertificateAlias = null,
            ),
            outgoingServerSettings = com.fsck.k9.mail.ServerSettings(
                type = "smtp",
                host = "smtp.example.com",
                port = 465,
                connectionSecurity = com.fsck.k9.mail.ConnectionSecurity.SSL_TLS_REQUIRED,
                authenticationType = com.fsck.k9.mail.AuthType.PLAIN,
                username = "test",
                password = "pass",
                clientCertificateAlias = null,
            ),
        )
    }

    private fun createViewModel(
        accountId: AccountId,
        initialState: SendingMailSettingContract.State = defaultState(),
        getLegacyAccount: suspend (
            AccountId,
        ) -> Outcome<
            LegacyAccount,
            AccountSettingsDomainContract.AccountSettingError,
            > = {
            Outcome.success(dummyLegacyAccount(it))
        },
        updateSendingMailSettings: suspend (
            AccountId,
            AccountSettingsDomainContract.UpdateSendingMailSettingsCommand,
        ) -> Outcome<
            Unit,
            AccountSettingsDomainContract.AccountSettingError,
            > = { _, _ ->
            Outcome.success(Unit)
        },
    ) = SendingMailSettingsViewModel(
        accountId = accountId,
        logger = TestLogger(),
        getAccountName = { flowOf(Outcome.success("Subtitle")) },
        getLegacyAccount = getLegacyAccount,
        updateSendingMailSettings = updateSendingMailSettings,
        initialState = initialState,
        optionsMapper = optionsMapper,
    )

    @Test
    fun `should navigate back when back pressed`() = runTest {
        val vm = createViewModel(AccountIdFactory.create())
        val effects = mutableListOf<SendingMailSettingContract.Effect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.event(SendingMailSettingContract.Event.OnBackPressed)
        advanceUntilIdle()

        assertThat(effects.first()).isEqualTo(SendingMailSettingContract.Effect.NavigateBack)
        job.cancel()
    }

    @Test
    fun `should navigate to composition defaults`() = runTest {
        val vm = createViewModel(AccountIdFactory.create())
        val effects = mutableListOf<SendingMailSettingContract.Effect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.event(SendingMailSettingContract.Event.OnCompositionDefaultsClick)
        advanceUntilIdle()

        assertThat(effects.first()).isEqualTo(SendingMailSettingContract.Effect.NavigateToCompositionDefaults)
        job.cancel()
    }

    @Test
    fun `should navigate to manage identities`() = runTest {
        val vm = createViewModel(AccountIdFactory.create())
        val effects = mutableListOf<SendingMailSettingContract.Effect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.event(SendingMailSettingContract.Event.OnManageIdentitiesClick)
        advanceUntilIdle()

        assertThat(effects.first()).isEqualTo(SendingMailSettingContract.Effect.NavigateToManageIdentities)
        job.cancel()
    }

    @Test
    fun `should navigate to outgoing server settings`() = runTest {
        val vm = createViewModel(AccountIdFactory.create())
        val effects = mutableListOf<SendingMailSettingContract.Effect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.event(SendingMailSettingContract.Event.OnOutgoingServerClick)
        advanceUntilIdle()

        assertThat(effects.first()).isEqualTo(SendingMailSettingContract.Effect.NavigateToOutGoingServerSettings)
        job.cancel()
    }

    @Test
    fun `should initialize state from legacy account`() = runTest {
        val accountId = AccountIdFactory.create()
        val vm = createViewModel(accountId)
        advanceUntilIdle()

        with(vm.state.value) {
            assertThat(messageFormat.id).isEqualTo(MessageFormat.HTML.name)
            assertThat(alwaysShowCcBcc).isEqualTo(true)
            assertThat(readReceipt).isEqualTo(true)
            assertThat(replyQuotingStyle.id).isEqualTo(QuoteStyle.HEADER.name)
            assertThat(quoteMessageWhenReplying).isEqualTo(true)
            assertThat(replyAfterQuotedText).isEqualTo(true)
            assertThat(stripSignatureOnReply).isEqualTo(true)
            assertThat(quotedTextPrefix).isEqualTo("> ")
            assertThat(uploadSentMessages).isEqualTo(true)
        }
    }

    @Test
    fun `should update subtitle when account name is loaded`() = runTest {
        val accountId = AccountIdFactory.create()
        val vm = SendingMailSettingsViewModel(
            accountId = accountId,
            logger = TestLogger(),
            getAccountName = { flowOf(Outcome.success("My Account")) },
            getLegacyAccount = { Outcome.success(dummyLegacyAccount(accountId)) },
            updateSendingMailSettings = { _, _ -> Outcome.success(Unit) },
            initialState = defaultState(),
            optionsMapper = optionsMapper,
        )

        advanceUntilIdle()
        assertThat(vm.state.value.subtitle).isEqualTo("My Account")
    }

    @Test
    fun `should update message format`() = runTest {
        val accountId = AccountIdFactory.create()
        var command: AccountSettingsDomainContract.UpdateSendingMailSettingsCommand? = null
        val vm = createViewModel(
            accountId = accountId,
            updateSendingMailSettings = { _, updateCommand ->
                command = updateCommand
                Outcome.success(Unit)
            },
        )
        val option = SelectOption("TEXT") { "Plain Text" }

        vm.event(SendingMailSettingContract.Event.OnMessageFormatChange(option))
        advanceUntilIdle()

        assertThat(command).isEqualTo(
            AccountSettingsDomainContract.UpdateSendingMailSettingsCommand.UpdateMessageFormat("TEXT"),
        )
        assertThat(vm.state.value.messageFormat.id).isEqualTo("TEXT")
    }

    @Test
    fun `should update always show cc bcc`() = runTest {
        val accountId = AccountIdFactory.create()
        var command: AccountSettingsDomainContract.UpdateSendingMailSettingsCommand? = null
        val vm = createViewModel(
            accountId = accountId,
            updateSendingMailSettings = { _, updateCommand ->
                command = updateCommand
                Outcome.success(Unit)
            },
        )

        vm.event(SendingMailSettingContract.Event.OnAlwaysShowCcBccToggle(true))
        advanceUntilIdle()

        assertThat(command).isEqualTo(
            AccountSettingsDomainContract.UpdateSendingMailSettingsCommand.UpdateAlwaysShowCcBcc(true),
        )
        assertThat(vm.state.value.alwaysShowCcBcc).isEqualTo(true)
    }

    @Test
    fun `should update read receipt`() = runTest {
        val accountId = AccountIdFactory.create()
        var command: AccountSettingsDomainContract.UpdateSendingMailSettingsCommand? = null
        val vm = createViewModel(
            accountId = accountId,
            updateSendingMailSettings = { _, updateCommand ->
                command = updateCommand
                Outcome.success(Unit)
            },
        )

        vm.event(SendingMailSettingContract.Event.OnReadReceiptToggle(true))
        advanceUntilIdle()

        assertThat(command).isEqualTo(
            AccountSettingsDomainContract.UpdateSendingMailSettingsCommand.UpdateReadReceipt(true),
        )
        assertThat(vm.state.value.readReceipt).isEqualTo(true)
    }

    @Test
    fun `should update reply quoting style`() = runTest {
        val accountId = AccountIdFactory.create()
        var command: AccountSettingsDomainContract.UpdateSendingMailSettingsCommand? = null
        val vm = createViewModel(
            accountId = accountId,
            updateSendingMailSettings = { _, updateCommand ->
                command = updateCommand
                Outcome.success(Unit)
            },
        )
        val option = SelectOption("HEADER") { "Header" }

        vm.event(SendingMailSettingContract.Event.OnReplyQuotingStyleChange(option))
        advanceUntilIdle()

        assertThat(command).isEqualTo(
            AccountSettingsDomainContract.UpdateSendingMailSettingsCommand.UpdateReplyQuotingStyle("HEADER"),
        )
        assertThat(vm.state.value.replyQuotingStyle.id).isEqualTo("HEADER")
    }

    @Test
    fun `should update quote message when replying`() = runTest {
        val accountId = AccountIdFactory.create()
        var command: AccountSettingsDomainContract.UpdateSendingMailSettingsCommand? = null
        val vm = createViewModel(
            accountId = accountId,
            updateSendingMailSettings = { _, updateCommand ->
                command = updateCommand
                Outcome.success(Unit)
            },
        )

        vm.event(SendingMailSettingContract.Event.OnQuoteMessageWhenReplyingToggle(true))
        advanceUntilIdle()

        assertThat(command).isEqualTo(
            AccountSettingsDomainContract.UpdateSendingMailSettingsCommand.UpdateQuoteMessageWhenReplying(true),
        )
        assertThat(vm.state.value.quoteMessageWhenReplying).isEqualTo(true)
    }

    @Test
    fun `should update reply after quoted text`() = runTest {
        val accountId = AccountIdFactory.create()
        var command: AccountSettingsDomainContract.UpdateSendingMailSettingsCommand? = null
        val vm = createViewModel(
            accountId = accountId,
            updateSendingMailSettings = { _, updateCommand ->
                command = updateCommand
                Outcome.success(Unit)
            },
        )

        vm.event(SendingMailSettingContract.Event.OnReplyAfterQuotedTextToggle(true))
        advanceUntilIdle()

        assertThat(command).isEqualTo(
            AccountSettingsDomainContract.UpdateSendingMailSettingsCommand.UpdateReplyAfterQuotedText(true),
        )
        assertThat(vm.state.value.replyAfterQuotedText).isEqualTo(true)
    }

    @Test
    fun `should update strip signature on reply`() = runTest {
        val accountId = AccountIdFactory.create()
        var command: AccountSettingsDomainContract.UpdateSendingMailSettingsCommand? = null
        val vm = createViewModel(
            accountId = accountId,
            updateSendingMailSettings = { _, updateCommand ->
                command = updateCommand
                Outcome.success(Unit)
            },
        )

        vm.event(SendingMailSettingContract.Event.OnStripSignatureOnReplyToggle(true))
        advanceUntilIdle()

        assertThat(command).isEqualTo(
            AccountSettingsDomainContract.UpdateSendingMailSettingsCommand.UpdateStripSignatureOnReply(true),
        )
        assertThat(vm.state.value.stripSignatureOnReply).isEqualTo(true)
    }

    @Test
    fun `should update quoted text prefix`() = runTest {
        val accountId = AccountIdFactory.create()
        var command: AccountSettingsDomainContract.UpdateSendingMailSettingsCommand? = null
        val vm = createViewModel(
            accountId = accountId,
            updateSendingMailSettings = { _, updateCommand ->
                command = updateCommand
                Outcome.success(Unit)
            },
        )

        vm.event(SendingMailSettingContract.Event.OnQuotedTextPrefixChange("> "))
        advanceUntilIdle()

        assertThat(command).isEqualTo(
            AccountSettingsDomainContract.UpdateSendingMailSettingsCommand.UpdateQuotedTextPrefix("> "),
        )
        assertThat(vm.state.value.quotedTextPrefix).isEqualTo("> ")
    }

    @Test
    fun `should update upload sent messages`() = runTest {
        val accountId = AccountIdFactory.create()
        var command: AccountSettingsDomainContract.UpdateSendingMailSettingsCommand? = null
        val vm = createViewModel(
            accountId = accountId,
            updateSendingMailSettings = { _, updateCommand ->
                command = updateCommand
                Outcome.success(Unit)
            },
        )

        vm.event(SendingMailSettingContract.Event.OnUploadSentMessagesToggle(true))
        advanceUntilIdle()

        assertThat(command).isEqualTo(
            AccountSettingsDomainContract.UpdateSendingMailSettingsCommand.UpdateUploadSentMessages(true),
        )
        assertThat(vm.state.value.uploadSentMessages).isEqualTo(true)
    }

    @Test
    fun `should keep default state when loading settings fails`() = runTest {
        val accountId = AccountIdFactory.create()
        val vm = createViewModel(
            accountId = accountId,
            getLegacyAccount = {
                Outcome.failure(
                    AccountSettingsDomainContract.AccountSettingError.StorageError("error"),
                )
            },
        )

        advanceUntilIdle()

        assertThat(vm.state.value.messageFormat.id).isEqualTo("TEXT")
        assertThat(vm.state.value.alwaysShowCcBcc).isEqualTo(false)
        assertThat(vm.state.value.stripSignatureOnReply).isEqualTo(false)
        assertThat(vm.state.value.quotedTextPrefix).isEqualTo(">")
    }

    @Test
    fun `should not update strip signature on reply when update fails`() = runTest {
        val accountId = AccountIdFactory.create()
        val vm = createViewModel(
            accountId = accountId,
            getLegacyAccount = {
                Outcome.success(
                    dummyLegacyAccount(
                        accountId = accountId,
                        isStripSignature = false,
                    ),
                )
            },
            updateSendingMailSettings = { _, _ ->
                Outcome.failure(
                    AccountSettingsDomainContract.AccountSettingError.StorageError("error"),
                )
            },
        )

        advanceUntilIdle()
        assertThat(vm.state.value.stripSignatureOnReply).isEqualTo(false)

        vm.event(SendingMailSettingContract.Event.OnStripSignatureOnReplyToggle(true))
        advanceUntilIdle()

        assertThat(vm.state.value.stripSignatureOnReply).isEqualTo(false)
    }
}
