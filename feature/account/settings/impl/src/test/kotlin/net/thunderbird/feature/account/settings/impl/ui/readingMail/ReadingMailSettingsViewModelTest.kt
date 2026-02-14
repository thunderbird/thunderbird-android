package net.thunderbird.feature.account.settings.impl.ui.readingMail

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.ShowPictures
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.testing.coroutines.MainDispatcherRule
import net.thunderbird.core.ui.setting.SettingValue.Select.SelectOption
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReadingMailSettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    /** Default initial state */
    private fun defaultState() = ReadingMailSettingsContract.State(
        subtitle = null,
        showPictures = SelectOption(ShowPictures.NEVER.name) { "never" },
        isMarkMessageAsReadOnView = false,
    )

    /** Dummy LegacyAccount builder for tests */
    private fun dummyLegacyAccount(
        accountId: AccountId,
        showPictures: ShowPictures = ShowPictures.NEVER,
        isMarkReadOnView: Boolean = false,
    ) = net.thunderbird.core.android.account.LegacyAccount(
        id = accountId,
        name = "Demo",
        email = "demo@example.com",
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
                description = "Demo Identity",
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
        showPictures = showPictures,
        isMarkMessageAsReadOnView = isMarkReadOnView,
    )

    /** Simple StringsResourceManager for tests */
    private val testResources = object : net.thunderbird.core.common.resources.StringsResourceManager {
        override fun stringResource(resourceId: Int): String = "string_$resourceId"
        override fun stringResource(resourceId: Int, vararg formatArgs: Any?): String = error("Not implemented")
    }

    /** Helper to create the ViewModel */
    private fun createViewModel(
        accountId: AccountId,
        state: ReadingMailSettingsContract.State = defaultState(),
        getLegacyAccount: suspend (
            AccountId,
        ) -> Outcome<LegacyAccount, AccountSettingsDomainContract.AccountSettingError> = {
            Outcome.success(dummyLegacyAccount(it))
        },
        updateReadMailSettings: suspend (
            AccountId,
            AccountSettingsDomainContract.UpdateReadMessageSettingsCommand,
        ) -> Outcome<Unit, AccountSettingsDomainContract.AccountSettingError> = { _, _ -> Outcome.success(Unit) },
    ) = ReadingMailSettingsViewModel(
        accountId = accountId,
        getAccountName = { flowOf(Outcome.success("Subtitle")) },
        getLegacyAccount = getLegacyAccount,
        updateReadMailSettings = updateReadMailSettings,
        logger = TestLogger(),
        resources = testResources,
        initialState = state,
    )

    @Test
    fun `should navigate back when back pressed`() = runTest {
        val accountId = AccountIdFactory.create()
        val vm = createViewModel(accountId)

        val effects = mutableListOf<ReadingMailSettingsContract.Effect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.event(ReadingMailSettingsContract.Event.OnBackPressed)
        advanceUntilIdle()

        assertThat(effects.first()).isEqualTo(ReadingMailSettingsContract.Effect.NavigateBack)
        job.cancel()
    }

    @Test
    fun `should map all show pictures options correctly on initialization`() = runTest {
        val accountId = AccountIdFactory.create()

        val options = listOf(
            ShowPictures.NEVER,
            ShowPictures.ALWAYS,
            ShowPictures.ONLY_FROM_CONTACTS,
        )

        options.forEach { showPictures ->
            val vm = createViewModel(
                accountId,
                getLegacyAccount = { Outcome.success(dummyLegacyAccount(accountId, showPictures)) },
            )
            advanceUntilIdle()
            assertThat(vm.state.value.showPictures.id).isEqualTo(showPictures.name)
        }
    }

    @Test
    fun `should update show pictures setting and state`() = runTest {
        val accountId = AccountIdFactory.create()
        var lastCommand: AccountSettingsDomainContract.UpdateReadMessageSettingsCommand? = null

        val vm = createViewModel(
            accountId,
            updateReadMailSettings = { _, command ->
                lastCommand = command
                Outcome.success(Unit)
            },
        )

        val option = SelectOption(ShowPictures.ALWAYS.name) { "always" }
        vm.event(ReadingMailSettingsContract.Event.OnShowPicturesChange(option))
        advanceUntilIdle()

        assertThat(lastCommand).isEqualTo(
            AccountSettingsDomainContract.UpdateReadMessageSettingsCommand.UpdateShowPictures(ShowPictures.ALWAYS.name),
        )
        assertThat(vm.state.value.showPictures.id).isEqualTo(ShowPictures.ALWAYS.name)
    }

    @Test
    fun `should update mark message as read on view and state`() = runTest {
        val accountId = AccountIdFactory.create()
        var lastCommand: AccountSettingsDomainContract.UpdateReadMessageSettingsCommand? = null

        val vm = createViewModel(
            accountId,
            updateReadMailSettings = { _, command ->
                lastCommand = command
                Outcome.success(Unit)
            },
        )

        vm.event(ReadingMailSettingsContract.Event.OnIsMarkMessageAsReadOnViewToggle(true))
        advanceUntilIdle()

        assertThat(lastCommand).isEqualTo(
            AccountSettingsDomainContract.UpdateReadMessageSettingsCommand.UpdateIsMarkMessageAsReadOnView(true),
        )
        assertThat(vm.state.value.isMarkMessageAsReadOnView).isEqualTo(true)
    }

    @Test
    fun `should update subtitle when account name is loaded`() = runTest {
        val accountId = AccountIdFactory.create()

        val vm = createViewModel(
            accountId,
            getLegacyAccount = { Outcome.success(dummyLegacyAccount(accountId)) },
            updateReadMailSettings = { _, _ -> Outcome.success(Unit) },
        )

        // Override getAccountName to emit a custom subtitle
        val vmWithSubtitle = ReadingMailSettingsViewModel(
            accountId = accountId,
            getAccountName = { flowOf(Outcome.success("My Account Name")) },
            getLegacyAccount = { Outcome.success(dummyLegacyAccount(accountId)) },
            updateReadMailSettings = { _, _ -> Outcome.success(Unit) },
            logger = TestLogger(),
            resources = testResources,
            initialState = defaultState(),
        )

        advanceUntilIdle()

        assertThat(vmWithSubtitle.state.value.subtitle).isEqualTo("My Account Name")
    }

    @Test
    fun `should rollback mark message as read on view if update fails`() = runTest {
        val accountId = AccountIdFactory.create()

        val vm = createViewModel(
            accountId,
            updateReadMailSettings = { _, _ ->
                Outcome.failure(AccountSettingsDomainContract.AccountSettingError.StorageError("fail"))
            },
        )

        vm.event(ReadingMailSettingsContract.Event.OnIsMarkMessageAsReadOnViewToggle(true))
        advanceUntilIdle()

        assertThat(vm.state.value.isMarkMessageAsReadOnView).isEqualTo(false)
    }
}
