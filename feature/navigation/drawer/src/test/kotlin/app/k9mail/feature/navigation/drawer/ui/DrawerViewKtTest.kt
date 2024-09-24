package app.k9mail.feature.navigation.drawer.ui

import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.printToString
import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.testing.onNodeWithTag
import app.k9mail.core.ui.compose.testing.setContentWithTheme
import app.k9mail.feature.navigation.drawer.ui.DrawerContract.Effect
import app.k9mail.feature.navigation.drawer.ui.DrawerContract.State
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class DrawerViewKtTest : ComposeTest() {

    @Test
    fun `should delegate effects`() = runTest {
        val initialState = State()
        val viewModel = FakeDrawerViewModel(initialState)
        val counter = Counter()
        val verifyCounter = Counter()

        setContentWithTheme {
            DrawerView(
                openAccount = { counter.openAccountCount++ },
                openFolder = { counter.openFolderCount++ },
                openUnifiedFolder = { counter.openUnifiedFolderCount++ },
                openManageFolders = { counter.openManageFoldersCount++ },
                openSettings = { counter.openSettingsCount++ },
                closeDrawer = { counter.closeDrawerCount++ },
                viewModel = viewModel,
            )
        }

        assertThat(counter).isEqualTo(verifyCounter)

        viewModel.effect(Effect.OpenAccount(FakeData.ACCOUNT))

        verifyCounter.openAccountCount++
        assertThat(counter).isEqualTo(verifyCounter)

        verifyCounter.openFolderCount++
        viewModel.effect(Effect.OpenFolder(1))

        verifyCounter.openUnifiedFolderCount++
        viewModel.effect(Effect.OpenUnifiedFolder)

        verifyCounter.openManageFoldersCount++
        viewModel.effect(Effect.OpenManageFolders)

        verifyCounter.openSettingsCount++
        viewModel.effect(Effect.OpenSettings)

        verifyCounter.closeDrawerCount++
        viewModel.effect(Effect.CloseDrawer)
    }

    @Test
    fun `pull refresh should listen to view model state`() = runTest {
        val initialState = State(
            isLoading = false,
        )
        val viewModel = FakeDrawerViewModel(initialState)

        setContentWithTheme {
            DrawerView(
                openAccount = {},
                openFolder = {},
                openUnifiedFolder = {},
                openManageFolders = {},
                openSettings = {},
                closeDrawer = {},
                viewModel = viewModel,
            )
        }

        onNodeWithTag("PullToRefreshBox").assertExists()
        onNodeWithTag("PullToRefreshIndicator").assertExists()
            .onChildAt(0).assertExists()
            .printToString()
            .contains("ProgressBarRangeInfo(current=0.0, range=0.0..1.0, steps=0)")

        viewModel.applyState(initialState.copy(isLoading = true))

        onNodeWithTag("PullToRefreshIndicator").assertExists()
            .onChildAt(0).assertExists()
            .printToString()
            .contains("ProgressBarRangeInfo(current=0.0, range=0.0..0.0, steps=0)")

        viewModel.applyState(initialState.copy(isLoading = false))

        onNodeWithTag("PullToRefreshIndicator").assertExists()
            .onChildAt(0).assertExists()
            .printToString()
            .contains("ProgressBarRangeInfo(current=0.0, range=0.0..1.0, steps=0)")
    }

    @Suppress("DataClassShouldBeImmutable")
    private data class Counter(
        var openAccountCount: Int = 0,
        var openFolderCount: Int = 0,
        var openUnifiedFolderCount: Int = 0,
        var openManageFoldersCount: Int = 0,
        var openSettingsCount: Int = 0,
        var closeDrawerCount: Int = 0,
    )
}
