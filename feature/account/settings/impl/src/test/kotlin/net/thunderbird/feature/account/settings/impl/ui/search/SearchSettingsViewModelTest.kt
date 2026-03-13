package net.thunderbird.feature.account.settings.impl.ui.search

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.ShowPictures
import net.thunderbird.core.common.resources.StringsResourceManager
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.testing.coroutines.MainDispatcherHelper
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract

@OptIn(ExperimentalCoroutinesApi::class)
class SearchSettingsViewModelTest {

    private val mainDispatcher = MainDispatcherHelper()

    @BeforeTest
    fun setUp() {
        mainDispatcher.setUp()
    }

    @AfterTest
    fun tearDown() {
        mainDispatcher.tearDown()
    }
    private fun dummyLegacyAccount(accountId: AccountId) =
        LegacyAccount(
            id = accountId,
            name = "Emon",
            email = "emon@gmail.com",
            isSensitiveDebugLoggingEnabled = { true },
            profile = net.thunderbird.feature.account.storage.profile.ProfileDto(
                id = accountId,
                name = "Emon",
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
            showPictures = ShowPictures.NEVER,
            isMarkMessageAsReadOnView = false,
            remoteSearchNumResults = 25,
        )

    private val testResources = object : StringsResourceManager {
        override fun stringResource(resourceId: Int): String =
            "string_$resourceId"

        override fun stringResource(resourceId: Int, vararg formatArgs: Any?): String =
            error("Not implemented")
    }

    private fun createViewModel(
        accountId: AccountId = AccountIdFactory.create(),
        getAccountName: AccountSettingsDomainContract.UseCase.GetAccountName,
        getLegacyAccount: AccountSettingsDomainContract.UseCase.GetLegacyAccount,
        updateSearchSettings: AccountSettingsDomainContract.UseCase.UpdateSearchSettings,
    ): SearchSettingsViewModel {
        return SearchSettingsViewModel(
            accountId = accountId,
            getAccountName = getAccountName,
            getLegacyAccount = getLegacyAccount,
            updateSearchSettings = updateSearchSettings,
            logger = TestLogger(),
            resources = testResources,
            dispatcher = mainDispatcher.testDispatcher,
        )
    }

    @Test
    fun `should initialize server search limit from legacy account`() = runTest {
        val vm = createViewModel(
            getAccountName = { flowOf(Outcome.success("My Account")) },
            getLegacyAccount = { Outcome.success(dummyLegacyAccount(it)) },
            updateSearchSettings = { _, _ -> Outcome.success(Unit) },
        )

        advanceUntilIdle()

        assertThat(vm.state.value.serverSearchLimit.id)
            .isEqualTo(ServerSearchLimit.TWENTY_FIVE.count.toString())
    }

    @Test
    fun `should emit navigate back effect when back pressed`() = runTest {
        val vm = createViewModel(
            getAccountName = { flowOf(Outcome.success("My Account")) },
            getLegacyAccount = { Outcome.success(dummyLegacyAccount(it)) },
            updateSearchSettings = { _, _ -> Outcome.success(Unit) },
        )

        vm.effect.test {
            vm.event(SearchSettingsContract.Event.OnBackPressed)

            advanceUntilIdle()

            assertThat(awaitItem())
                .isEqualTo(SearchSettingsContract.Effect.NavigateBack)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should update server search limit and update state on success`() = runTest {
        var capturedCommand: AccountSettingsDomainContract.UpdateSearchSettingsCommand? = null

        val vm = createViewModel(
            getAccountName = { flowOf(Outcome.success("My Account")) },
            getLegacyAccount = { Outcome.success(dummyLegacyAccount(it)) },
            updateSearchSettings = { _, command ->
                capturedCommand = command
                Outcome.success(Unit)
            },
        )

        val newLimit = ServerSearchLimit.HUNDRED

        vm.event(
            SearchSettingsContract.Event.OnServerSearchLimitChange(
                newLimit.count,
            ),
        )

        advanceUntilIdle()

        assertThat(capturedCommand).isEqualTo(
            AccountSettingsDomainContract.UpdateSearchSettingsCommand
                .UpdateServerSearchLimit(newLimit.count),
        )

        assertThat(vm.state.value.serverSearchLimit.id)
            .isEqualTo(newLimit.count.toString())
    }

    @Test
    fun `should NOT update state when updateSearchSettings fails`() = runTest {
        val vm = createViewModel(
            getAccountName = { flowOf(Outcome.success("My Account")) },
            getLegacyAccount = { Outcome.success(dummyLegacyAccount(it)) },
            updateSearchSettings = { _, _ ->
                Outcome.failure(
                    AccountSettingsDomainContract.AccountSettingError.StorageError("fail"),
                )
            },
        )

        val originalState = vm.state.value.serverSearchLimit.id

        vm.event(
            SearchSettingsContract.Event.OnServerSearchLimitChange(
                ServerSearchLimit.FIFTY.count,
            ),
        )

        advanceUntilIdle()

        assertThat(vm.state.value.serverSearchLimit.id)
            .isEqualTo(originalState)
    }

    @Test
    fun `should update subtitle when account name emits`() = runTest {
        val accountName = "My Account"

        val vm = createViewModel(
            getAccountName = { flowOf(Outcome.success(accountName)) },
            getLegacyAccount = { Outcome.success(dummyLegacyAccount(it)) },
            updateSearchSettings = { _, _ -> Outcome.success(Unit) },
        )

        advanceUntilIdle()

        assertThat(vm.state.value.subtitle)
            .isEqualTo(accountName)
    }
}
