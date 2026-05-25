package net.thunderbird.feature.account.settings.impl.ui.fetchingMail

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
import net.thunderbird.core.android.account.DeletePolicy
import net.thunderbird.core.android.account.Expunge
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.testing.coroutines.MainDispatcherHelper
import net.thunderbird.core.ui.setting.SettingValue.Select.SelectOption
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract

@OptIn(ExperimentalCoroutinesApi::class)
internal class FetchingMailSettingsViewModelTest {

    private val mainDispatcher = MainDispatcherHelper()

    @BeforeTest
    fun setUp() {
        mainDispatcher.setUp()
    }

    @AfterTest
    fun tearDown() {
        mainDispatcher.tearDown()
    }

    private val resources = object : net.thunderbird.core.common.resources.StringsResourceManager {
        override fun stringResource(resourceId: Int): String = "string_$resourceId"

        override fun stringResource(resourceId: Int, vararg formatArgs: Any?): String {
            return stringResource(resourceId)
        }
    }

    private fun defaultState() = FetchingMailSettingsContract.State(
        subtitle = null,
        localFolderSize = SelectOption("10") { "" },
        syncMessageFrom = SelectOption("-1") { "" },
        fetchMessageUpTo = SelectOption("1024") { "" },
        folderPollFrequency = SelectOption("-1") { "" },
        syncServerDeletions = false,
        markAsReadWhenDeleted = false,
        whenIDeleteAMessage = SelectOption(DeletePolicy.NEVER.name) { "" },
        eraseDeletedMessageOnServer = SelectOption(Expunge.EXPUNGE_IMMEDIATELY.name) { "" },
        maxFolderToCheckWithPush = SelectOption("5") { "" },
        refreshIdleConnection = SelectOption("2") { "" },
    )

    private fun dummyLegacyAccount(
        accountId: AccountId,
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
                net.thunderbird.core.android.account.Identity(
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
        initialState: FetchingMailSettingsContract.State = defaultState(),
        getLegacyAccount: suspend (
            AccountId,
        ) -> Outcome<LegacyAccount, AccountSettingsDomainContract.AccountSettingError> = {
            Outcome.success(dummyLegacyAccount(it))
        },
        updateFetchingMailSettings: suspend (
            AccountId,
            AccountSettingsDomainContract.UpdateFetchingMailSettingsCommand,
        ) -> Outcome<Unit, AccountSettingsDomainContract.AccountSettingError> = { _, _ ->
            Outcome.success(Unit)
        },
    ) = FetchingMailSettingsViewModel(
        accountId = accountId,
        logger = TestLogger(),
        resources = resources,
        getAccountName = { flowOf(Outcome.success("Subtitle")) },
        getLegacyAccount = getLegacyAccount,
        updateFetchingMailSettings = updateFetchingMailSettings,
        initialState = initialState,
    )

    @Test
    fun `should navigate back when back pressed`() = runTest {
        val vm = createViewModel(AccountIdFactory.create())

        val effects = mutableListOf<FetchingMailSettingsContract.Effect>()

        val job = launch {
            vm.effect.collect {
                effects.add(it)
            }
        }

        vm.event(FetchingMailSettingsContract.Event.OnBackPressed)

        advanceUntilIdle()

        assertThat(effects.first())
            .isEqualTo(FetchingMailSettingsContract.Effect.NavigateBack)

        job.cancel()
    }

    @Test
    fun `should navigate to incoming server settings`() = runTest {
        val vm = createViewModel(AccountIdFactory.create())

        val effects = mutableListOf<FetchingMailSettingsContract.Effect>()

        val job = launch {
            vm.effect.collect {
                effects.add(it)
            }
        }

        vm.event(FetchingMailSettingsContract.Event.OnInComingServerClick)

        advanceUntilIdle()

        assertThat(effects.first())
            .isEqualTo(
                FetchingMailSettingsContract.Effect.NavigateToIncomingServerSettings,
            )

        job.cancel()
    }

    @Test
    fun `should navigate to advanced fetching mail settings`() = runTest {
        val vm = createViewModel(AccountIdFactory.create())

        val effects = mutableListOf<FetchingMailSettingsContract.Effect>()

        val job = launch {
            vm.effect.collect {
                effects.add(it)
            }
        }

        vm.event(FetchingMailSettingsContract.Event.OnAdvanceClick)

        advanceUntilIdle()

        assertThat(effects.first())
            .isEqualTo(
                FetchingMailSettingsContract.Effect.NavigateToAdvancedFetchingMailSettings,
            )

        job.cancel()
    }

    @Test
    fun `should initialize state from legacy account`() = runTest {
        val accountId = AccountIdFactory.create()

        val vm = createViewModel(accountId)

        advanceUntilIdle()

        with(vm.state.value) {
            assertThat(localFolderSize.id).isEqualTo("500")
            assertThat(syncMessageFrom.id).isEqualTo("365")
            assertThat(fetchMessageUpTo.id).isEqualTo("5242880")
            assertThat(folderPollFrequency.id).isEqualTo("720")
            assertThat(syncServerDeletions).isEqualTo(true)
            assertThat(markAsReadWhenDeleted).isEqualTo(true)
            assertThat(whenIDeleteAMessage.id)
                .isEqualTo(DeletePolicy.MARK_AS_READ.name)

            assertThat(eraseDeletedMessageOnServer.id)
                .isEqualTo(Expunge.EXPUNGE_MANUALLY.name)

            assertThat(maxFolderToCheckWithPush.id).isEqualTo("250")
            assertThat(refreshIdleConnection.id).isEqualTo("48")
        }
    }

    @Test
    fun `should update subtitle when account name is loaded`() = runTest {
        val accountId = AccountIdFactory.create()

        val vm = FetchingMailSettingsViewModel(
            accountId = accountId,
            logger = TestLogger(),
            resources = resources,
            getAccountName = {
                flowOf(Outcome.success("My Account"))
            },
            getLegacyAccount = {
                Outcome.success(dummyLegacyAccount(accountId))
            },
            updateFetchingMailSettings = { _, _ ->
                Outcome.success(Unit)
            },
            initialState = defaultState(),
        )

        advanceUntilIdle()

        assertThat(vm.state.value.subtitle)
            .isEqualTo("My Account")
    }

    @Test
    fun `should update local folder size`() = runTest {
        val accountId = AccountIdFactory.create()

        var command:
            AccountSettingsDomainContract.UpdateFetchingMailSettingsCommand? = null

        val vm = createViewModel(
            accountId = accountId,
            updateFetchingMailSettings = { _, updateCommand ->
                command = updateCommand
                Outcome.success(Unit)
            },
        )

        val option = SelectOption("1000") { "1000" }

        vm.event(
            FetchingMailSettingsContract.Event.OnLocalFolderSizeChange(option),
        )

        advanceUntilIdle()

        assertThat(command).isEqualTo(
            AccountSettingsDomainContract
                .UpdateFetchingMailSettingsCommand
                .UpdateLocalFolderSize(1000),
        )

        assertThat(vm.state.value.localFolderSize.id)
            .isEqualTo("1000")
    }

    @Test
    fun `should update sync message from`() = runTest {
        val accountId = AccountIdFactory.create()

        var command:
            AccountSettingsDomainContract.UpdateFetchingMailSettingsCommand? = null

        val vm = createViewModel(
            accountId = accountId,
            updateFetchingMailSettings = { _, updateCommand ->
                command = updateCommand
                Outcome.success(Unit)
            },
        )

        val option = SelectOption("28") { "28" }

        vm.event(
            FetchingMailSettingsContract.Event.OnSyncMessageFromChange(option),
        )

        advanceUntilIdle()

        assertThat(command).isEqualTo(
            AccountSettingsDomainContract
                .UpdateFetchingMailSettingsCommand
                .UpdateSyncMessageFrom(28),
        )

        assertThat(vm.state.value.syncMessageFrom.id)
            .isEqualTo("28")
    }

    @Test
    fun `should update fetch message up to`() = runTest {
        val accountId = AccountIdFactory.create()

        var command:
            AccountSettingsDomainContract.UpdateFetchingMailSettingsCommand? = null

        val vm = createViewModel(
            accountId = accountId,
            updateFetchingMailSettings = { _, updateCommand ->
                command = updateCommand
                Outcome.success(Unit)
            },
        )

        val option = SelectOption("2097152") { "2097152" }

        vm.event(
            FetchingMailSettingsContract.Event.OnFetchMessageUpToChange(option),
        )

        advanceUntilIdle()

        assertThat(command).isEqualTo(
            AccountSettingsDomainContract
                .UpdateFetchingMailSettingsCommand
                .UpdateFetchMessageUpTo(2097152),
        )

        assertThat(vm.state.value.fetchMessageUpTo.id)
            .isEqualTo("2097152")
    }

    @Test
    fun `should update folder poll frequency`() = runTest {
        val accountId = AccountIdFactory.create()

        var command:
            AccountSettingsDomainContract.UpdateFetchingMailSettingsCommand? = null

        val vm = createViewModel(
            accountId = accountId,
            updateFetchingMailSettings = { _, updateCommand ->
                command = updateCommand
                Outcome.success(Unit)
            },
        )

        val option = SelectOption("360") { "360" }

        vm.event(
            FetchingMailSettingsContract.Event.OnFolderPollFrequencyChange(option),
        )

        advanceUntilIdle()

        assertThat(command).isEqualTo(
            AccountSettingsDomainContract
                .UpdateFetchingMailSettingsCommand
                .UpdateFolderPollFrequency(360),
        )

        assertThat(vm.state.value.folderPollFrequency.id)
            .isEqualTo("360")
    }

    @Test
    fun `should update sync server deletions`() = runTest {
        val accountId = AccountIdFactory.create()

        var command:
            AccountSettingsDomainContract.UpdateFetchingMailSettingsCommand? = null

        val vm = createViewModel(
            accountId = accountId,
            updateFetchingMailSettings = { _, updateCommand ->
                command = updateCommand
                Outcome.success(Unit)
            },
        )

        vm.event(
            FetchingMailSettingsContract.Event.OnSyncServerDeletionsToggle(true),
        )

        advanceUntilIdle()

        assertThat(command).isEqualTo(
            AccountSettingsDomainContract
                .UpdateFetchingMailSettingsCommand
                .UpdateSyncServerDeletions(true),
        )

        assertThat(vm.state.value.syncServerDeletions)
            .isEqualTo(true)
    }

    @Test
    fun `should update mark as read when deleted`() = runTest {
        val accountId = AccountIdFactory.create()

        var command:
            AccountSettingsDomainContract.UpdateFetchingMailSettingsCommand? = null

        val vm = createViewModel(
            accountId = accountId,
            updateFetchingMailSettings = { _, updateCommand ->
                command = updateCommand
                Outcome.success(Unit)
            },
        )

        vm.event(
            FetchingMailSettingsContract.Event.OnMarkAsReadWhenDeletedToggle(true),
        )

        advanceUntilIdle()

        assertThat(command).isEqualTo(
            AccountSettingsDomainContract
                .UpdateFetchingMailSettingsCommand
                .UpdateMarkAsReadWhenDeleted(true),
        )

        assertThat(vm.state.value.markAsReadWhenDeleted)
            .isEqualTo(true)
    }

    @Test
    fun `should update delete policy`() = runTest {
        val accountId = AccountIdFactory.create()

        var command:
            AccountSettingsDomainContract.UpdateFetchingMailSettingsCommand? = null

        val vm = createViewModel(
            accountId = accountId,
            updateFetchingMailSettings = { _, updateCommand ->
                command = updateCommand
                Outcome.success(Unit)
            },
        )

        val option = SelectOption(DeletePolicy.ON_DELETE.name) { "" }

        vm.event(
            FetchingMailSettingsContract.Event.OnWhenIDeleteAMessageChange(option),
        )

        advanceUntilIdle()

        assertThat(command).isEqualTo(
            AccountSettingsDomainContract
                .UpdateFetchingMailSettingsCommand
                .UpdateWhenIDeleteAMessage(DeletePolicy.ON_DELETE.name),
        )

        assertThat(vm.state.value.whenIDeleteAMessage.id)
            .isEqualTo(DeletePolicy.ON_DELETE.name)
    }

    @Test
    fun `should update expunge policy`() = runTest {
        val accountId = AccountIdFactory.create()

        var command:
            AccountSettingsDomainContract.UpdateFetchingMailSettingsCommand? = null

        val vm = createViewModel(
            accountId = accountId,
            updateFetchingMailSettings = { _, updateCommand ->
                command = updateCommand
                Outcome.success(Unit)
            },
        )

        val option = SelectOption(Expunge.EXPUNGE_ON_POLL.name) { "" }

        vm.event(
            FetchingMailSettingsContract.Event
                .OnEraseDeletedMessageOnServerChange(option),
        )

        advanceUntilIdle()

        assertThat(command).isEqualTo(
            AccountSettingsDomainContract
                .UpdateFetchingMailSettingsCommand
                .UpdateEraseDeletedMessageOnServer(
                    Expunge.EXPUNGE_ON_POLL.name,
                ),
        )

        assertThat(vm.state.value.eraseDeletedMessageOnServer.id)
            .isEqualTo(Expunge.EXPUNGE_ON_POLL.name)
    }

    @Test
    fun `should update max folder to check with push`() = runTest {
        val accountId = AccountIdFactory.create()

        var command:
            AccountSettingsDomainContract.UpdateFetchingMailSettingsCommand? = null

        val vm = createViewModel(
            accountId = accountId,
            updateFetchingMailSettings = { _, updateCommand ->
                command = updateCommand
                Outcome.success(Unit)
            },
        )

        val option = SelectOption("100") { "" }

        vm.event(
            FetchingMailSettingsContract.Event
                .OnMaxFolderToCheckWithPushChange(option),
        )

        advanceUntilIdle()

        assertThat(command).isEqualTo(
            AccountSettingsDomainContract
                .UpdateFetchingMailSettingsCommand
                .UpdateOnMaxFolderToCheckWithPushChange(100),
        )

        assertThat(vm.state.value.maxFolderToCheckWithPush.id)
            .isEqualTo("100")
    }

    @Test
    fun `should update refresh idle connection frequency`() = runTest {
        val accountId = AccountIdFactory.create()

        var command:
            AccountSettingsDomainContract.UpdateFetchingMailSettingsCommand? = null

        val vm = createViewModel(
            accountId = accountId,
            updateFetchingMailSettings = { _, updateCommand ->
                command = updateCommand
                Outcome.success(Unit)
            },
        )

        val option = SelectOption("24") { "" }

        vm.event(
            FetchingMailSettingsContract.Event
                .OnRefreshIdleConnectionFrequencyChange(option),
        )

        advanceUntilIdle()

        assertThat(command).isEqualTo(
            AccountSettingsDomainContract
                .UpdateFetchingMailSettingsCommand
                .UpdateRefreshIdleConnectionFrequencyChange(24),
        )

        assertThat(vm.state.value.refreshIdleConnection.id)
            .isEqualTo("24")
    }

    @Test
    fun `should keep default state when loading settings fails`() = runTest {
        val accountId = AccountIdFactory.create()

        val vm = createViewModel(
            accountId = accountId,
            getLegacyAccount = {
                Outcome.failure(
                    AccountSettingsDomainContract.AccountSettingError.StorageError(
                        "error",
                    ),
                )
            },
        )

        advanceUntilIdle()

        assertThat(vm.state.value.localFolderSize.id)
            .isEqualTo("10")

        assertThat(vm.state.value.syncServerDeletions)
            .isEqualTo(false)
    }

    @Test
    fun `should not update local folder size when update fails`() = runTest {
        val accountId = AccountIdFactory.create()

        val vm = createViewModel(
            accountId = accountId,
            updateFetchingMailSettings = { _, _ ->
                Outcome.failure(
                    AccountSettingsDomainContract.AccountSettingError.StorageError(
                        "error",
                    ),
                )
            },
        )

        vm.event(
            FetchingMailSettingsContract.Event.OnLocalFolderSizeChange(
                SelectOption("1000") { "" },
            ),
        )

        advanceUntilIdle()

        assertThat(vm.state.value.localFolderSize.id)
            .isEqualTo("500")
    }
}
