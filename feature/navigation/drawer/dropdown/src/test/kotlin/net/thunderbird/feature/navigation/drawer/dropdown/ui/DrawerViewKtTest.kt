package net.thunderbird.feature.navigation.drawer.dropdown.ui

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.testing.setContentWithTheme
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import net.thunderbird.account.fake.FakeAccountData.ACCOUNT_ID_RAW
import net.thunderbird.feature.navigation.drawer.dropdown.FolderDrawerState
import net.thunderbird.feature.navigation.drawer.dropdown.ui.DrawerContract.Effect
import net.thunderbird.feature.navigation.drawer.dropdown.ui.DrawerContract.Event
import net.thunderbird.feature.navigation.drawer.dropdown.ui.DrawerContract.State

internal class DrawerViewKtTest : ComposeTest() {

    @Test
    fun `should delegate effects`() = runTest {
        val initialState = State()
        val viewModel = FakeDrawerViewModel(initialState)
        val counter = Counter()
        val verifyCounter = Counter()

        setContentWithTheme {
            DrawerView(
                drawerState = FolderDrawerState(),
                openAccount = { counter.openAccountCount++ },
                openFolder = { _, _ -> counter.openFolderCount++ },
                openUnifiedFolder = { counter.openUnifiedFolderCount++ },
                openManageFolders = { counter.openManageFoldersCount++ },
                openSettings = { counter.openSettingsCount++ },
                openAddAccount = { counter.openAddAccountCount++ },
                closeDrawer = { counter.closeDrawerCount++ },
                featureFlagProvider = FakeFeatureFlagProvider(isEnabled = true),
                viewModel = viewModel,
            )
        }

        assertThat(counter).isEqualTo(verifyCounter)

        viewModel.effect(Effect.OpenAccount(FakeData.MAIL_DISPLAY_ACCOUNT.id))

        verifyCounter.openAccountCount++
        assertThat(counter).isEqualTo(verifyCounter)

        verifyCounter.openFolderCount++
        viewModel.effect(
            Effect.OpenFolder(
                accountId = ACCOUNT_ID_RAW,
                folderId = 1,
            ),
        )

        verifyCounter.openUnifiedFolderCount++
        viewModel.effect(Effect.OpenUnifiedFolder)

        verifyCounter.openManageFoldersCount++
        viewModel.effect(Effect.OpenManageFolders)

        verifyCounter.openSettingsCount++
        viewModel.effect(Effect.OpenSettings)

        verifyCounter.closeDrawerCount++
        viewModel.effect(Effect.CloseDrawer)

        verifyCounter.openAddAccountCount++
        viewModel.effect(Effect.OpenAddAccount)
    }

    @Test
    fun `should register to drawer state and send events to view model`() = runTest {
        val initialState = State()
        val viewModel = FakeDrawerViewModel(initialState)
        val initialDrawerState = FolderDrawerState()
        val drawerStateFlow = MutableStateFlow(initialDrawerState)

        setContentWithTheme {
            val state = drawerStateFlow.collectAsStateWithLifecycle()

            DrawerView(
                drawerState = state.value,
                openAccount = { },
                openFolder = { _, _ -> },
                openUnifiedFolder = { },
                openManageFolders = { },
                openSettings = { },
                openAddAccount = { },
                closeDrawer = { },
                featureFlagProvider = FakeFeatureFlagProvider(isEnabled = true),
                viewModel = viewModel,
            )
        }

        drawerStateFlow.emit(initialDrawerState.copy(selectedAccountUuid = FakeData.ACCOUNT.uuid))

        viewModel.events.contains(Event.SelectAccount(FakeData.ACCOUNT.uuid))

        drawerStateFlow.emit(initialDrawerState.copy(selectedAccountUuid = null))

        viewModel.events.contains(Event.SelectAccount(null))

        drawerStateFlow.emit(initialDrawerState.copy(selectedFolderId = "1"))

        viewModel.events.contains(Event.SelectFolder("1"))

        drawerStateFlow.emit(initialDrawerState.copy(selectedFolderId = null))

        viewModel.events.contains(Event.SelectFolder(null))
    }

    @Suppress("DataClassShouldBeImmutable")
    private data class Counter(
        var openAccountCount: Int = 0,
        var openFolderCount: Int = 0,
        var openUnifiedFolderCount: Int = 0,
        var openManageFoldersCount: Int = 0,
        var openSettingsCount: Int = 0,
        var openAddAccountCount: Int = 0,
        var closeDrawerCount: Int = 0,
    )
}
