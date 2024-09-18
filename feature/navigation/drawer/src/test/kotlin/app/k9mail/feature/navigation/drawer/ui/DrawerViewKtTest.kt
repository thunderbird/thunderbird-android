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
        var openAccountCounter = 0
        var openFolderCounter = 0
        var openManageFoldersCounter = 0
        var openSettingsCounter = 0
        var closeDrawerCounter = 0

        setContentWithTheme {
            DrawerView(
                openAccount = { openAccountCounter++ },
                openFolder = { openFolderCounter++ },
                openManageFolders = { openManageFoldersCounter++ },
                openSettings = { openSettingsCounter++ },
                closeDrawer = { closeDrawerCounter++ },
                viewModel = viewModel,
            )
        }

        assertThat(openAccountCounter).isEqualTo(0)
        assertThat(openFolderCounter).isEqualTo(0)
        assertThat(openManageFoldersCounter).isEqualTo(0)
        assertThat(openSettingsCounter).isEqualTo(0)
        assertThat(closeDrawerCounter).isEqualTo(0)

        viewModel.effect(Effect.OpenAccount(FakeData.ACCOUNT))

        assertThat(openAccountCounter).isEqualTo(1)
        assertThat(openFolderCounter).isEqualTo(0)
        assertThat(openManageFoldersCounter).isEqualTo(0)
        assertThat(openSettingsCounter).isEqualTo(0)
        assertThat(closeDrawerCounter).isEqualTo(0)

        viewModel.effect(Effect.OpenFolder(1))

        assertThat(openAccountCounter).isEqualTo(1)
        assertThat(openFolderCounter).isEqualTo(1)
        assertThat(openManageFoldersCounter).isEqualTo(0)
        assertThat(openSettingsCounter).isEqualTo(0)
        assertThat(closeDrawerCounter).isEqualTo(0)

        viewModel.effect(Effect.OpenManageFolders)

        assertThat(openAccountCounter).isEqualTo(1)
        assertThat(openFolderCounter).isEqualTo(1)
        assertThat(openManageFoldersCounter).isEqualTo(1)
        assertThat(openSettingsCounter).isEqualTo(0)
        assertThat(closeDrawerCounter).isEqualTo(0)

        viewModel.effect(Effect.OpenSettings)

        assertThat(openAccountCounter).isEqualTo(1)
        assertThat(openFolderCounter).isEqualTo(1)
        assertThat(openManageFoldersCounter).isEqualTo(1)
        assertThat(openSettingsCounter).isEqualTo(1)
        assertThat(closeDrawerCounter).isEqualTo(0)

        viewModel.effect(Effect.CloseDrawer)

        assertThat(openAccountCounter).isEqualTo(1)
        assertThat(openFolderCounter).isEqualTo(1)
        assertThat(openManageFoldersCounter).isEqualTo(1)
        assertThat(openSettingsCounter).isEqualTo(1)
        assertThat(closeDrawerCounter).isEqualTo(1)
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
}
